node ('nimble-jenkins-slave') {

    stage('Clone and Update') {
        git(url: 'https://github.com/nimble-platform/catalog-service-srdc.git', branch: env.BRANCH_NAME)
    }

    stage('Build Dependencies') {
        sh 'git clone https://github.com/nimble-platform/common'
        dir ('common') {
            sh 'mvn clean install'
        }
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
        sh 'kubectl apply -f kubernetes/deploy.yml -n prod --validate=false'
    }
}
