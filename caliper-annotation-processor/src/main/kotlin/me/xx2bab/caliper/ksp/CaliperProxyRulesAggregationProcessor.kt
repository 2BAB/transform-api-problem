package me.xx2bab.caliper.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.javapoet.KotlinPoetJavaPoetPreview
import com.squareup.kotlinpoet.javapoet.toJTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import me.xx2bab.caliper.anno.*
import kotlin.collections.getOrPut
import me.xx2bab.caliper.common.Constants.KSP_OPTION_ANDROID_APP
import java.util.concurrent.atomic.AtomicBoolean

class CaliperProxyRulesAggregationProcessorProvider : SymbolProcessorProvider {
    override fun create(
        env: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return CaliperProxyRulesAggregationProcessor(
            env.codeGenerator, env.logger
        )
    }
}

/**
 * The processor to aggregate all [@ProxyMethod] [@ProxyField] from [caliper-runtime] module.
 */
class CaliperProxyRulesAggregationProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("process")

        // Load metadata from subprojects, put all of them into the `exportMetadata` of main project,
        val methodAndFieldSymbols =
            resolver.getSymbolsWithAnnotation(CaliperMethodProxy::class.qualifiedName!!)
                .plus(resolver.getSymbolsWithAnnotation(CaliperFieldProxy::class.qualifiedName!!))
        logger.info("Method + Field symbols: ${methodAndFieldSymbols.toList().size}")
        methodAndFieldSymbols.filter { it is KSFunctionDeclaration && it.validate() }
            .forEach {
                // it.accept(MetaCollectorForMethodAndFieldProxy(), Unit)
                // ... skip some processing code snippet, can goto `finish()` to see the generation code
            }

        // To simplify the workflow, we only support one round processing,
        // since all proxy class are designated to be fixed (resolved elements).
        return emptyList()
    }

    override fun finish() {
        super.finish()
        logger.info("finish")
        // val generator = CaliperWrapperGenerator(metadataMap, codeGenerator, logger)
        val file = codeGenerator.createNewFile(Dependencies.ALL_FILES, "", "aggregation", "json")
        file.write(
            ("{\n" +
                    "  \"proxiedMethods\": [\n" +
                    "    {\n" +
                    "      \"className\": \"LibrarySampleClass\",\n" +
                    "      \"methodName\": \"commonMethodReturnString\",\n" +
                    "      \"opcode\": 182,\n" +
                    "      \"replacedClassName\": \"me/xx2bab/caliper/runtime/wrapper/CustomProxy_CaliperWrapper\",\n" +
                    "      \"replacedMethodName\": \"commonMethodReturnsString\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}").toByteArray()
        )
        file.close()
    }

}