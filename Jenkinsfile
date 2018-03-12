node('nimble-jenkins-slave') {

    stage('Clone and Update') {
        git(url: 'https://github.com/nimble-platform/catalog-service.git', branch: env.BRANCH_NAME)
    }

    stage('Build Dependencies') {
        sh 'rm -rf common   '
        sh 'git clone https://github.com/nimble-platform/common'
        dir('common') {
            sh 'mvn clean install'
        }
    }

    stage('Build Java') {
        sh '/bin/bash -xe deploy.sh java-build'
    }

    if (env.BRANCH_NAME == 'staging') {
        stage('Build Docker') {
            sh '/bin/bash -xe deploy.sh docker-build-staging'
        }

        stage('Push Docker') {
            sh 'docker push nimbleplatform/catalogue-service-micro:staging'
        }

        stage('Deploy') {
            sh 'ssh staging "cd /srv/nimble-staging/ && ./run-staging.sh restart-single catalog-service-srdc"'
        }
    } else {
        stage('Build Docker') {
            sh '/bin/bash -xe deploy.sh docker-build'
        }
    }

    if (env.BRANCH_NAME == 'master') {
        stage('Deploy') {
            sh 'ssh nimble "cd /data/deployment_setup/prod/ && sudo ./run-prod.sh restart-single catalog-service-srdc"'
        }
    }

    // Kubernetes setup is disabled for now
//    if (env.BRANCH_NAME == 'master') {
//        stage('Push Docker') {
//            withDockerRegistry([credentialsId: 'NimbleDocker']) {
//                sh '/bin/bash -xe deploy.sh docker-push'
//            }
//        }
//
//        stage('Apply to Cluster') {
//            sh 'kubectl apply -f kubernetes/deploy.yml -n prod --validate=false'
//        }
//    }
}
