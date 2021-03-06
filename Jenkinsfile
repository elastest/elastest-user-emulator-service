node('dev-docker-64-slave-4') {
    stage "Container Prep"
        try {
            echo("The node is up")
            def mycontainer = docker.image('elastest/ci-docker-siblings:latest')
            mycontainer.pull()
            mycontainer.inside("-u 0 -v /var/run/docker.sock:/var/run/docker.sock:rw -v /var/lib/jenkins/caches/durable-task:/var/lib/jenkins/caches/durable-task:rw") {

                def epmClientJavaDirectory = 'epm-client-java'

                git 'https://github.com/elastest/elastest-user-emulator-service.git'

                stage "Install et-epm-client-java"
                    def epmClientDirectoryExists = fileExists epmClientJavaDirectory
                    if (epmClientDirectoryExists) {
                        echo 'EPM client directory exists'
                    } else {
                        echo 'There is not EPM client directory. Creating...'
                        sh 'mkdir ' + epmClientJavaDirectory
                    }
                    sh 'chmod 777 ' + epmClientJavaDirectory
                        
                    dir(epmClientJavaDirectory) {
                        echo 'Existing files before cloning the git repository'
                        git 'https://github.com/mpauls/epm-client-java.git'
                    }
                        
                    echo 'Installing epm-client-java'
                    sh "ls -lrt; cd $epmClientJavaDirectory; mvn clean install -Dmaven.test.skip=true"

                def etmJavaDirectory = 'etm-java'
                stage "Test and deploy epm-client"
                    def etmDirectoryExists = fileExists etmJavaDirectory
                    if (etmDirectoryExists) {
                         echo 'EPM client directory exists'
                    } else {
                         echo 'There is not ETM directory. Creating...'
                         sh 'mkdir ' + etmJavaDirectory
                    }
                    sh 'chmod 777 ' + etmJavaDirectory
                      
                    dir(etmJavaDirectory) {
                         echo 'Existing files before cloning the git repository'
                         git 'https://github.com/elastest/elastest-torm.git'
                    }
                    
                    echo ("Test and deploy epm-client")
                    sh "cd $etmJavaDirectory; cd ./epm-client; mvn install -DskipTests -Dgpg.skip -Djenkins=true;"



                stage "Tests"
                    echo ("Starting tests")
                    try {
                        sh 'cd eus; mvn clean -Dspring.profiles.active=required,notdependency -Djenkins=true -Det.files.path.in.host=/tmp/eus/ -Det.data.in.host=/tmp/ -Det.shared.folder=/tmp/ test' 
                    } catch (err) {
			sh 'ls /tmp/eus/shared_files'
                        def errString = err.toString()
                        currentBuild.result = getJobStatus(errString)
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
            if (currentBuild.result != "UNSTABLE") {
                def errString = err.toString()
                echo 'Error: ' + errString
               	currentBuild.result = getJobStatus(errString)
            }
            echo 'Error!!! Send email to the people responsible for the builds.'
            emailext body: 'Please go to  ${BUILD_URL}  and verify the build',
            replyTo: '${BUILD_USER_EMAIL}', 
            subject: 'Job ${JOB_NAME} - ${BUILD_NUMBER} RESULT: ${BUILD_STATUS}', 
            to: '${MAIL_LIST}'

        throw err
    }
}

def getJobStatus(exceptionString) {
    def status = 'SUCCESS'
    if (exceptionString.contains('FlowInterruptedException') || exceptionString.contains('AbortException')) {
        status = 'ABORTED'
    } else {
        status = 'FAILURE'
    }
    return status;
}
