node('docker') {
    stage "Container Prep"
        echo("The node is up")
        def mycontainer = docker.image('elastest/docker-in-docker:latest')
        mycontainer.pull()
        mycontainer.inside("-u jenkins -v /var/run/docker.sock:/var/run/docker.sock:rw") {
            git 'https://github.com/elastest/elastest-user-emulator-service.git'

            stage "Tests"
                echo ("Starting tests")
                sh 'cd elastest-eus; mvn clean test'
                step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])

            stage "Package"
                echo ("Packaging")
                sh 'cd elastest-eus; mvn package -DskipTests'

            stage "Archive artifacts"
                archiveArtifacts artifacts: 'eus/target/*.jar'

            stage "Build Docker images"
                echo ("Building")
                def eusImage = docker.build -f eus/Dockerfile 'elastest/eus'
                def novncImage = docker.build -f novnc/Dockerfile 'elastest/novnc'

            stage "Run images"
                eusImage.run()
                novncImage.run()

            stage "Publish"
                echo ("Publishing")
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'elastestci-dockerhub',
                    usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                    sh 'docker login -u "$USERNAME" -p "$PASSWORD"'
                    eusImage.push()
                    novncImage.push()
                }
        }
}
