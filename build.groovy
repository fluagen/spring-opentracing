#!groovy

@Library('EdgeLabJenkins') _

properties([
        disableConcurrentBuilds(),
        buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '60', numToKeepStr: '300'))
])

def build = "spring-opentracing"
def workerImage = "edgelab/marketdata-ci:maven"
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

def replacePomVersion(version) {
    sh "sed -i -e '/version/s/${getPomVersion()}/${version}/' ${env.WORKSPACE}/pom.xml"
    sh "git commit ${env.WORKSPACE}/pom.xml -m '[ci skip] update version to ${version}'"
}

def getPomVersion() {
    def pom = readMavenPom file: "${env.WORKSPACE}/pom.xml"
    pom.version
}
