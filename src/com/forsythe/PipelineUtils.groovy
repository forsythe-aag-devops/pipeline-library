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

}