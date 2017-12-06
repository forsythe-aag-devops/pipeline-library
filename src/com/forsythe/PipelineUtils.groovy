#!/usr/bin/groovy
package com.forsythe
import com.cloudbees.groovy.cps.NonCPS

class PipelineUtils implements Serializable {

    def steps
    PipelineUtils(steps) {this.steps = steps}

    @NonCPS
    def extractNamespace(jobName) {
        return jobName.tokenize('/')[0]
    }

    @NonCPS
    def waitForValidNamespaceState(String namespace) {
        waitForAllPodsRunning(namespace)
        waitForAllServicesRunning(namespace)
    }

    @NonCPS
    def waitForAllPodsRunning(String namespace) {
        steps.timeout(time: 3, unit: 'MINUTES') {
            while (true) {
                def proc = "sh kubectl --namespace='${namespace}' get pods --no-headers".execute()
                def podsStatus = proc.in.text.trim()
                def notRunning = podsStatus.readLines().findAll { line -> !line.contains('Running') }
                if (notRunning.isEmpty()) {
                    echo 'All pods are running'
                    break
                }
                steps.sh "kubectl --namespace='${namespace}' get pods"
                steps.sleep 10
            }
        }
    }

    @NonCPS
    def waitForAllServicesRunning(String namespace) {
        steps.timeout(time: 3, unit: 'MINUTES') {
            while (true) {
                def proc = "sh kubectl --namespace='${namespace}' get services --no-headers".execute()
                def servicesStatus = proc.in.text.trim()
                def notRunning = servicesStatus.readLines().findAll { line -> line.contains('pending') }
                if (notRunning.isEmpty()) {
                    echo 'All pods are running'
                    break
                }
                steps.sh "kubectl --namespace='${namespace}' get services"
                steps.sleep 10
            }
        }
    }

    @NonCPS
    def createNamespace(String namespace) {
        steps.sh "kubectl create namespace ${namespace} || true"
    }

    @NonCPS
    def deleteNamespace(String namespace) {
        steps.sh "kubectl delete namespace ${namespace} --ignore-not-found=true"
    }

    @NonCPS
    def extractServiceEndpoint(String namespace, String serviceName) {
        nexusEndpoint = steps.sh(returnStdout: true, script: "kubectl --namespace='${namespace}' get svc ${serviceName} --no-headers --template '{{ range (index .status.loadBalancer.ingress 0) }}{{ . }}{{ end }}'").trim()
    }

    @NonCPS
    def analyzeCode(String jobName, String srcDirectory) {
        try {
            steps.echo "downloading scanner-cli"
            steps.sh "curl -o scanner-cli.jar http://central.maven.org/maven2/org/sonarsource/scanner/cli/sonar-scanner-cli/2.8/sonar-scanner-cli-2.8.jar"
            steps.echo "executing sonar scanner "
            def projectKey = jobName.replaceAll('/', "_")
            steps.sh "java -jar scanner-cli.jar -Dsonar.host.url=http://sonarqube:9000  -Dsonar.projectKey=${projectKey} -Dsonar.projectBaseDir=${srcDirectory} -Dsonar.java.binaries=${srcDirectory}/target/classes -Dsonar.sources=${srcDirectory}"
        } catch (err) {
            print "Failed to execute scanner:"
            print "Exception: ${err}"
            throw err
        }
    }
}