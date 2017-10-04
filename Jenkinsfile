node ('nimble-jenkins-slave') {
    def app
    stage('Clone and Update') {
        // slackSend 'Started build no. ${env.BUILD_ID} of ${env.JOB_NAME}'
        git(url: 'https://github.com/nimble-platform/catalog-service-srdc.git', branch: 'master')
    }

    stage ('Build Docker Image') {
        sh '/bin/bash -xe deploy.sh docker-build'
    }

    stage ('Push Docker image') {
        withDockerRegistry([credentialsId: 'NimbleDocker']) {
            sh '/bin/bash -xe deploy.sh docker-push'
        }
    }

    stage ('Apply to Cluster') {
        sh 'kubectl apply -f kubernetes/deploy.yaml -n prod --validate=false'
    }
}
