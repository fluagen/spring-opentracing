#!groovy

@Library('EdgeLabJenkins') _

properties([
        disableConcurrentBuilds()
])

def build = "spring-opentracing"
def workerImage = "edgelab/jenkins-worker:v11.4.2"
def flavor = "default" // flavor of the AWS instance

awsDockerNode(build, flavor, workerImage) {
    def ci = new ch.edgelab.CI()

    stage('Checkout branch') {
        checkout scm
    }

    if (ci.shouldSkipCI(currentBuild)) {
        return
    }

    if (env.BRANCH_NAME == 'master') {
        sshagent(credentials: ['jenkins-ssh-key']) {
            replacePomVersion(version())
            deploy()
            sh "git push origin HEAD:master"
        }

        report()
    } else {
        runTests()
    }

    findbugs pattern: '**/findbugsXml.xml'
}

def runTests() {
    stage("Test") {
        configFileProvider([configFile(fileId: 'marketdata-maven-settings', variable: 'MAVEN_SETTINGS')]) {
            sh "mvn --batch-mode --settings ${env.MAVEN_SETTINGS} clean install -am findbugs:findbugs"
        }
    }
}

def deploy() {
    stage("Deploy artifact") {
        configFileProvider([configFile(fileId: 'marketdata-maven-settings', variable: 'MAVEN_SETTINGS')]) {
            sh "mvn --batch-mode --settings ${env.MAVEN_SETTINGS} clean deploy -am findbugs:findbugs"
        }
    }
}

def report() {
    stage("Codacy report") {
        withEnv(["CODACY_PROJECT_TOKEN=6a129aee1ef94db0a76d620eb7972116"]) {
            sh "curl -Ls -o codacy-coverage-reporter-assembly.jar " +
                    "\$(curl -Ls https://api.github.com/repos/codacy/codacy-coverage-reporter/releases/latest | " +
                    "jq -r '.assets | " +
                    "map({content_type, browser_download_url} | " +
                    "select(.content_type | " +
                    "contains(\"java-archive\"))) | " +
                    ".[0].browser_download_url')"

            sh "java -jar codacy-coverage-reporter-assembly.jar report -l Java -r ${env.WORKSPACE}/target/site/jacoco/jacoco.xml"
        }
    }
}

def replacePomVersion(version) {
    sh "sed -i -e '/version/s/${getPomVersion()}/${version}/' ${env.WORKSPACE}/pom.xml"
    sh "git commit ${env.WORKSPACE}/pom.xml -m '[ci skip] update version to ${version}'"
}

def getPomVersion() {
    def pom = readMavenPom file: "${env.WORKSPACE}/pom.xml"
    pom.version
}
