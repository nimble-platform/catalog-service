node ('nimble-jenkins-slave') {
    stage('Clone and Update') {
        git(url: 'https://github.com/nimble-platform/catalog-service.git', branch: 'k8s-integration')

        sh 'rm -rf common ; git clone https://github.com/nimble-platform/common.git ; cd common ; mvn clean install'
    }

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

    stage ('Deploy') {
        sh ''' sed -i 's/IMAGE_TAG/'"$BUILD_NUMBER"'/g' kubernetes/deploy.yml '''
        sh 'kubectl apply -f kubernetes/deploy.yml -n prod --validate=false'
        sh 'kubectl apply -f kubernetes/svc.yml -n prod --validate=false'
    }

    stage ('Print-deploy logs') {
        sh 'sleep 60'
        sh 'kubectl -n prod logs deploy/catalogue-service -c catalogue-service'
    }
}