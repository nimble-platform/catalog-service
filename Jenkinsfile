node ('nimble-jenkins-slave') {
    stage('Clone and Update') {
        git(url: 'https://github.com/nimble-platform/catalog-service.git', branch: env.BRANCH_NAME)
    }

    stage('Build Dependencies') {
        sh 'rm -rf common'
        sh 'git clone https://github.com/nimble-platform/common'
        dir('common') {
            sh 'git checkout ' + env.BRANCH_NAME
            sh 'mvn clean install'
        }
    }
  
        sh 'rm -rf common ; git clone https://github.com/nimble-platform/common.git ; cd common ; mvn clean install'
    

    stage ('Build docker image') {
        sh 'mvn clean install -DskipTests'
        sh 'mvn -f catalogue-service-micro/pom.xml docker:build -DdockerImageTag=${BUILD_NUMBER} -P docker'

        sh 'sleep 5' // For the tag to populate
    }

    stage ('Push docker image') {
        withDockerRegistry([credentialsId: 'NimbleDocker']) {
            sh 'docker push nimbleplatform/catalogue-service-micro:${BUILD_NUMBER}'
        }
    }

    if (env.BRANCH_NAME == 'master') {

        stage('Push Docker') {
            sh 'docker push nimbleplatform/catalogue-service-micro:latest'
        }

        stage('Deploy') {
            sh 'ssh nimble "cd /data/deployment_setup/prod/ && sudo ./run-prod.sh restart-single catalog-service-srdc"'
        }
    }

    stage ('Print-deploy logs') {
        sh 'sleep 60'
        sh 'kubectl -n prod logs deploy/catalogue-service -c catalogue-service'
    }
}
