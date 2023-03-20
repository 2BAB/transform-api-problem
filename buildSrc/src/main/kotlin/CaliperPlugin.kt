import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.api.instrumentation.*
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.coverage.JacocoReportTask.JacocoReportWorkerAction.Companion.logger
import com.android.build.gradle.internal.tasks.DexMergingTask
import com.android.build.gradle.internal.tasks.MergeJavaResourceTask
import com.android.build.gradle.tasks.TransformClassesWithAsmTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
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

                // The new way: comment out below code snippet to see the exception
                appVariant.instrumentation
                    .transformClassesWith(
                        CaliperClassVisitorFactory::class.java,
                        InstrumentationScope.ALL
                    ) {
                        it.configJson.set(aggregationJson)
                        it.variantCaliperConfiguration.from(variantCaliperConfiguration)
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

        @get:Classpath
        val variantCaliperConfiguration: ConfigurableFileCollection
    }

    abstract class CaliperClassVisitorFactory :
        AsmClassVisitorFactory<ExampleParams> {

        override fun createClassVisitor(
            classContext: ClassContext,
            nextClassVisitor: ClassVisitor
        ): ClassVisitor {
            val configJson = parameters.get().configJson.get().asFile

            parameters.get().variantCaliperConfiguration.forEach {
                logger.lifecycle(" - ${it.absolutePath}")
                val jar = JarFile(it)
                jar.entries().asSequence().first {
                    it.name == "aggregation.json"
                }.apply {
                    // just pick up the latest one as it's just a demo
                    configJson.writeBytes(jar.getInputStream(this).readBytes())
                }
            }

            configJson.let {
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


