#!/usr/bin/groovy
package com.forsythe

def extractNamespace() {
    return "${env.JOB_NAME}".tokenize('/')[0]
}

def waitForValidNamespaceState(String namespace) {
    waitForAllPodsRunning(namespace)
    waitForAllServicesRunning(namespace)
}

def waitForAllPodsRunning(String namespace) {
    timeout(time: 3, unit: 'MINUTES') {
        while (true) {
            podsStatus = sh(returnStdout: true, script: "kubectl --namespace='${namespace}' get pods --no-headers").trim()
            def notRunning = podsStatus.readLines().findAll { line -> !line.contains('Running') }
            if (notRunning.isEmpty()) {
                echo 'All pods are running'
                break
            }
            sh "kubectl --namespace='${namespace}' get pods"
            sleep 10
        }
    }
}

def waitForAllServicesRunning(String namespace) {
    timeout(time: 3, unit: 'MINUTES') {
        while (true) {
            servicesStatus = sh(returnStdout: true, script: "kubectl --namespace='${namespace}' get services --no-headers").trim()
            def notRunning = servicesStatus.readLines().findAll { line -> line.contains('pending') }
            if (notRunning.isEmpty()) {
                echo 'All pods are running'
                break
            }
            sh "kubectl --namespace='${namespace}' get services"
            sleep 10
        }
    }
}

def createNamespace(String namespace) {
    try {
        sh "kubectl create namespace ${projectNamespace} || true"
    } catch(Exception ex) {}
}

def deleteNamespace(String namespace) {
    sh "kubectl delete namespace ${projectNamespace} --ignore-not-found=true"
}

def extractServiceEndpoint(String namespace, String serviceName) {
    nexusEndpoint = sh(returnStdout: true, script: "kubectl --namespace='${namespace}' get svc ${serviceName} --no-headers --template '{{ range (index .status.loadBalancer.ingress 0) }}{{ . }}{{ end }}'").trim()
}

return this
