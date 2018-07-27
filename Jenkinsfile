node('TESTDOCKER') {
    stage "Container Prep"
        try {
            echo("The node is up")
            def mycontainer = docker.image('elastest/ci-docker-siblings:latest')
            mycontainer.pull()
            mycontainer.inside("-u jenkins -v /var/run/docker.sock:/var/run/docker.sock:rw") {
                git 'https://github.com/elastest/elastest-user-emulator-service.git'

                stage "Tests"
                    echo ("Starting tests")
                    try {
                        sh 'cd eus; mvn clean test -Dspring.profiles.active=required,notdependency -Djenkins=true -Det.files.path=/tmp/'                        
                    }catch (err) {
                        currentBuild.result = "UNSTABLE"
                        throw err                    
                    } finally {
                        step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
                    }

                stage "Package"
                    echo ("Packaging")
                    sh 'cd eus; mvn clean package install -Pnormal -DskipTests -Dgpg.skip -Det.files.path=/tmp/'

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
        } catch (err) {
            echo 'Error!!! Send email to the people responsible for the builds.'
            emailext body: 'Please go to  ${BUILD_URL}  and verify the build',
            replyTo: '${BUILD_USER_EMAIL}', 
            subject: 'Job ${JOB_NAME} - ${BUILD_NUMBER} RESULT: ${BUILD_STATUS}', 
            to: '${MAIL_LIST}'

        throw err
    }	       
}
