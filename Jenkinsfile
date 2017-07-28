node('docker') {
    stage "Container Prep"
        echo("The node is up")
        def mycontainer = docker.image('elastest/docker-in-docker:latest')
        mycontainer.pull()
        mycontainer.inside("-u jenkins -v /var/run/docker.sock:/var/run/docker.sock:rw") {
            git 'https://github.com/elastest/elastest-user-emulator-service.git'

            stage "Tests"
                echo ("Starting tests")
                sh 'cd elastest-eus; mvn clean test -Ddocker.server.url=unix:///var/run/docker.sock'
                step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])

            stage "Package"
                echo ("Packaging")
                sh 'cd elastest-eus; mvn package -DskipTests'

            stage "Archive atifacts"
                archiveArtifacts artifacts: 'elastest-eus/target/*.jar'

            stage "Build image - Package"
                echo ("Building")
                def myimage = docker.build 'elastest/elastest-user-emulator-service'

            stage "Run image"
                myimage.run()

            stage "Publish"
                echo ("Publishing")
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'elastestci-dockerhub',
                    usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                    sh 'docker login -u "$USERNAME" -p "$PASSWORD"'
                    myimage.push()
                }
        }
}
