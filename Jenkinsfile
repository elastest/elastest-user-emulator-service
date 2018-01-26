node('TESTDOCKER') {
    stage "Container Prep"
        echo("The node is up")
        def mycontainer = docker.image('elastest/ci-docker-siblings:latest')
        mycontainer.pull()
        mycontainer.inside("-u jenkins -v /var/run/docker.sock:/var/run/docker.sock:rw") {
            git 'https://github.com/elastest/elastest-user-emulator-service.git'

            stage "Tests"
                echo ("Starting tests")
                try {
                    sh 'cd eus; mvn clean test -Djenkins=true'
                } finally {
                    step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
                }

            stage "Package"
                echo ("Packaging")
                sh 'cd eus; mvn package -DskipTests'

            stage "Archive artifacts"
                archiveArtifacts artifacts: 'eus/target/*.jar'

            stage "Build Docker images"
                echo ("Building")
                sh 'docker build --build-arg GIT_COMMIT=$(git rev-parse HEAD) --build-arg COMMIT_DATE=$(git log -1 --format=%cd --date=format:%Y-%m-%dT%H:%M:%S) -f eus/Dockerfile . -t elastest/eus'
                def eusImage = docker.image('elastest/eus')

            stage "Run images"
                eusImage.run()

            stage "Publish"
                echo ("Publishing")
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'elastestci-dockerhub',
                    usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                    sh 'docker login -u "$USERNAME" -p "$PASSWORD"'
                    eusImage.push()
                }
        }
}
