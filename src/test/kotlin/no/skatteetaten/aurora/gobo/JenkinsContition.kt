package no.skatteetaten.aurora.gobo

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext

@Target(AnnotationTarget.CLASS)
@Retention
@ExtendWith(DisableIfJenkinsCondition::class)
annotation class DisableIfJenkins

class DisableIfJenkinsCondition : ExecutionCondition {
    override fun evaluateExecutionCondition(context: ExtensionContext?): ConditionEvaluationResult {
        return if (System.getenv("JENKINS_HOME") == null) {
            ConditionEvaluationResult.enabled("Enabled integration tests")
        } else {
            ConditionEvaluationResult.disabled("Disabling integration tests on Jenkins")
        }
    }
}
