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
                podsStatus = steps.sh(returnStdout: true, script: "kubectl --namespace='${namespace}' get pods --no-headers").trim()
                def notRunning = podsStatus.readLines().findAll { line -> !line.contains('Running') }
                if (notRunning.isEmpty()) {
                    print 'All pods are running'
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
                servicesStatus = steps.sh(returnStdout: true, script: "kubectl --namespace='${namespace}' get services --no-headers").trim()
                def notRunning = servicesStatus.readLines().findAll { line -> line.contains('pending') }
                if (notRunning.isEmpty()) {
                    print 'All pods are running'
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
    def analyzeCode(String jobName) {
        try {
            def srcDirectory = steps.pwd()
            def tmpDir = steps.pwd(tmp: true)
            steps.dir(tmpDir) {
                def scannerVersion = "2.8"
                def localScanner = "scanner-cli.jar"
                def scannerURL = "http://central.maven.org/maven2/org/sonarsource/scanner/cli/sonar-scanner-cli/${scannerVersion}/sonar-scanner-cli-${scannerVersion}.jar"
                steps.echo "downloading scanner-cli"
                steps.sh "curl -o ${localScanner} ${scannerURL} "
                steps.echo "executing sonar scanner "
                def projectKey = jobName.replaceAll('/', "_")
                steps.sh "java -jar ${localScanner} -Dsonar.host.url=http://sonarqube:9000  -Dsonar.projectKey=${projectKey} -Dsonar.projectBaseDir=${srcDirectory} -Dsonar.java.binaries=${srcDirectory}/target/classes -Dsonar.sources=${srcDirectory}"
            }

        } catch (err) {
            print "Failed to execute scanner:"
            print "Exception: ${err}"
            throw err
        }
    }
}