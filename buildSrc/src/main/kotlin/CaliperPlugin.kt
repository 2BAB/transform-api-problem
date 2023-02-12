import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.api.instrumentation.*
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.coverage.JacocoReportTask.JacocoReportWorkerAction.Companion.logger
import com.android.build.gradle.internal.tasks.DexMergingTask
import com.android.build.gradle.tasks.TransformClassesWithAsmTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.withType
import org.objectweb.asm.ClassVisitor
import java.util.jar.JarFile


@CacheableTask
abstract class CaliperPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.withType<AppPlugin> {
            val caliperConfiguration =
                project.configurations.maybeCreate("caliper").apply {
                    description =
                        "Used by Caliper Gradle Plugin to gather metadata for bytecode transform.."
                    isCanBeConsumed = true
                }
            project.configurations.getByName("implementation")
                .extendsFrom(caliperConfiguration)

            val androidExtension =
                project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            androidExtension.onVariants { appVariant ->
                val capVariantName = appVariant.name.capitalized()
                // For Android .aar package + Variant filter
                val artifactType = Attribute.of("artifactType", String::class.java)
                val variantCaliperConfiguration = project.configurations
                    .maybeCreate("${appVariant.name}Caliper")
                    .apply {
                        extendsFrom(caliperConfiguration)
                        isTransitive = false
                        attributes {
                            attribute(
                                BuildTypeAttr.ATTRIBUTE,
                                project.objects.named(
                                    BuildTypeAttr::class.java,
                                    appVariant.buildType.toString()
                                )
                            )
                            attribute(artifactType, "android-java-res")
                        }
                    }

                val aggregationJson = project.layout.buildDirectory.file("caliper-aggregation.json")
                project.afterEvaluate { // to locate the "pre${capVariantName}Build" task we have to wait the evaluation ends
                    val javaResMergeTaskProvider = project.tasks
                        .getByName("merge${capVariantName}JavaResource")
                    javaResMergeTaskProvider.doLast {
                        variantCaliperConfiguration.incoming
                            .artifacts
                            .artifactFiles
                            .files
                            .forEach {
                                logger.lifecycle("$name - ${it.absolutePath}")
                                val jar = JarFile(it.absolutePath)
                                jar.entries().asSequence().first {
                                    it.name == "aggregation.json"
                                }.apply {
                                    // just pick up the latest one as it's just a demo
                                    aggregationJson.get()
                                        .asFile
                                        .writeBytes(jar.getInputStream(this).readBytes())
                                }
                            }
                    }
                }

                // The legacy way is an AGP task running right after the Kotlin/Java compilation,
                // here is just a mock up since the API is gone.
                project.afterEvaluate {
                    val legacyTransform =
                        project.tasks.register("mockup${capVariantName}ByteCodeTransform") {
                            inputs.file(aggregationJson)
                            dependsOn("compile${capVariantName}Kotlin")
                            dependsOn("merge${capVariantName}JavaResource")
                            doLast {
                                aggregationJson.get().asFile.let {
                                    logger.lifecycle("mockup transform with param: ${it.name}")
                                    //!! THE MOST IMPORTANT THING IS WE WANT TO PASS AN AGGREGATION FILE INTO THE TRANSFORMER.!!
                                    logger.lifecycle(it.readText())
                                    //... main logic are omitted
                                }
                            }
                        }
                    project.tasks.named("assemble${capVariantName}")
                        .configure { dependsOn(legacyTransform) }
                }

                // The new way: comment out below code snippet to see the exception
                appVariant.instrumentation
                    .transformClassesWith(
                        CaliperClassVisitorFactory::class.java,
                        InstrumentationScope.ALL
                    ) {
                        it.configJson.set(aggregationJson)
                    }
                appVariant.instrumentation
                    .setAsmFramesComputationMode(FramesComputationMode.COPY_FRAMES)

                project.afterEvaluate {
                    project.tasks.withType<DexMergingTask>()
                        .filter { it.variantName.equals(appVariant.name, ignoreCase = true) }
                        .forEach { dexMergeTask ->
                            dexMergeTask.dependsOn("merge${capVariantName}JavaResource")
                        }
                    project.tasks.withType<TransformClassesWithAsmTask>()
                        .filter { it.variantName.equals(appVariant.name, ignoreCase = true) }
                        .forEach { task ->
                            task.dependsOn("merge${capVariantName}JavaResource")
                        }

                }

            }
        }
    }

    interface ExampleParams : InstrumentationParameters {
        @get:InputFile
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val configJson: RegularFileProperty
    }

    abstract class CaliperClassVisitorFactory :
        AsmClassVisitorFactory<ExampleParams> {

        override fun createClassVisitor(
            classContext: ClassContext,
            nextClassVisitor: ClassVisitor
        ): ClassVisitor {
            parameters.get().configJson.get().asFile.let {
                //!! THE MOST IMPORTANT THING IS WE WANT TO PASS AN AGGREGATION FILE INTO THE TRANSFORMER.!!
                logger.lifecycle(it.readText())
            }
            return nextClassVisitor
        }

        override fun isInstrumentable(classData: ClassData): Boolean {
            return true// classData.className.startsWith("me.xx2bab.caliper.sample.library")
        }
    }


}


