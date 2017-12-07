#!/usr/bin/groovy
def call(config) {
    def namespace = config.namespace ?: "default"
    def serviceName = config.serviceName ?: "MicroService"
    def serviceId = config.serviceId
    def servicePort = config.port ?: "8080"

    try {
        def serviceEndpoint = sh(returnStdout: true, script: "kubectl --namespace='${namespace}' get svc ${serviceId} --no-headers --template '{{ range (index .status.loadBalancer.ingress 0) }}{{ . }}{{ end }}'").trim()
        echo "${serviceName} deployed at: "
        print "${serviceName} can be accessed at: http://${serviceEndpoint}:${servicePort}"
    } catch (err) {
        echo "Failed to get endpoint"
        echo "Exception: ${err}"
        throw err
    }
}