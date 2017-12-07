#!/usr/bin/groovy
def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    def namespace = config.namespace ?: "default";

    try {
        timeout(time: 10, unit: 'MINUTES') {
            while (true) {
                def podsStatus = sh(returnStdout: true, script: "kubectl --namespace='${namespace}' get pods --no-headers").trim()
                def notRunning = podsStatus.readLines().findAll { line -> !line.contains('Running') }
                if (notRunning.isEmpty()) {
                    echo 'All pods are running'
                    break
                }
                sh "kubectl --namespace='${namespace}' get pods"
                sleep 10
            }

            while (true) {
                def servicesStatus = sh(returnStdout: true, script: "kubectl --namespace='${namespace}' get services --no-headers").trim()
                def notRunning = servicesStatus.readLines().findAll { line -> line.contains('pending') }
                if (notRunning.isEmpty()) {
                    echo 'All pods are running'
                    break
                }
                sh "kubectl --namespace='${namespace}' get services"
                sleep 10
            }
        }
    } catch (err) {
        echo "The cluster is in wrong state:"
        echo "Exception: ${err}"
        throw err
    }
}