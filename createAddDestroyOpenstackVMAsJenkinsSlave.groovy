import org.apache.commons.lang3.StringUtils
import jenkins.*
import jenkins.model.*
import hudson.* 
import hudson.model.*

MACHINE_NAME = "openstack-test" + (new Random().nextInt()%7 + 85)

def checkWaitingBuildsOnBuildMachines(){
    stage('Waiting builds at <label Name> machines'){
        count = 0
        Hudson.instance.queue.items.each { 
            if(it.getAssignedLabel().toString() == '<label Name>'){
                job_name = StringUtils.substringBetween(it.toString(), "[", "]");
                println(job_name + " waiting at " + it.getAssignedLabel() + " for " + it.getInQueueForString())
                count++
            }
        } 
        return count
    }
}

def deleteOpenstackVM(IP){
    withCredentials([string(credentialsId: '<cred id>', variable: 'OS_PASSWORD')]) {
        env.OS_PASSWORD = "${OS_PASSWORD}"
        println("deleting Openstack VM " + IP)
        dir('openstack'){
            sh """
                . ./venv3/bin/activate
                IMAGE_ID=\$(openstack server list --ip "${IP}"|grep "${IP}"|cut -d'|' -f2)
                if [[ -n "\$IMAGE_ID" ]]
                   openstack server delete \$IMAGE_ID
                fi  
            """
        }
    }    
}

def removeOpenstackSlave(slave){
    println("removing Jenkins slave " + slave)
    Jenkins.instance.removeNode(slave)
}

def checkAndDestroyOpenstackVM(){
    stage('Check and Destroy Openstack VM and Jenkins slave'){
        jenkins = Hudson.instance
        ipAddressRegex = ~/([0-9]+\.[0-9]+\.[0-9]+\.[0-9]+)/
        for (int i =0; i < jenkins.slaves.size(); i++) {
            if(ipAddressRegex.matcher(jenkins.slaves[i].computer.name).matches()){
                if(jenkins.slaves[i].labelString == '<label name>'){
                    println jenkins.slaves[i].computer.name 
                    println jenkins.slaves[i].labelString
                    removeOpenstackSlave(jenkins.slaves[i])
                    deleteOpenstackVM(jenkins.slaves[i].computer.name.trim())
                }
            } 
        }  
    }
}

node('<label>'){
    timestamps {
        env.OS_AUTH_URL = "<openstack api url>"
        env.OS_PROJECT_ID = "<openstach project id>"
        env.OS_PROJECT_NAME = "<openstack project name>"
        env.OS_USER_DOMAIN_NAME = "<user domain on openstack>"
        env.OS_PROJECT_DOMAIN_ID = "<openstack project domain id>"
        env.OS_USERNAME = "<openstack username>"
        env.OS_REGION_NAME = "<region name>"
        env.OS_INTERFACE = "public"
        env.OS_IDENTITY_API_VERSION = 3
        
        stage("create-openstack-dir"){
            sh 'mkdir -p openstack'
        }
        stage('set up env'){
            dir('openstack'){
                sh """
                   python3 -m venv venv3
                   . ./venv3/bin/activate
                   pip install --upgrade pip
                   pip install python-openstackclient
                   openstack --version
                """
            }
        }
        
        if(checkWaitingBuildsOnDockerMahines() > 0){    
            stage('create openstack vm'){
                withCredentials([string(credentialsId: '<cred id>', variable: 'OS_PASSWORD')]) {
                    env.OS_PASSWORD = "${OS_PASSWORD}"
                    dir('openstack'){
                        sh """
                          . ./venv3/bin/activate
                          openstack server create --flavor "<flavor name>" --image "<image id>" --key-name "<key-pair name>" ${MACHINE_NAME}
                        """
                    }
                }
            }
            println("going to sleep for 300 seconds , waiting for VM to come up with sshd service up and running ....")
            sleep 300
            stage('Copy slave.jar on OS VM'){
                env.JENKINS_URL="<jenkins url>"
                env.NODE_SLAVE_HOME='<home dir>'
                dir('openstack'){
                    env.NODE_IP=sh(returnStdout: true, script: ". ./venv3/bin/activate; openstack server show ${MACHINE_NAME} | grep addresses|cut -d'=' -f2|cut -d'|' -f1").trim()
                    println("NODE IP of created OpenStack VM : ${NODE_IP}")
                    sh "curl -s -k ${env.JENKINS_URL}/jnlpJars/slave.jar -o slave.jar"
                    sh "chmod 777 slave.jar"
                    sh "ssh -o StrictHostKeyChecking=no  <username>@${NODE_IP} mkdir -p ${env.NODE_SLAVE_HOME}"
                    sh "scp -o StrictHostKeyChecking=no -p slave.jar <username>@${NODE_IP}:${env.NODE_SLAVE_HOME}"
                }
            }
            stage("Add ${NODE_IP} slave to jenkins slave"){
                env.JAVA_HOME = tool name: '<tool name on jenkins>', type: 'jdk'
                env.JENKINS_URL="<jenkins url>"
                env.NODE_SLAVE_HOME='<home dir>'
                env.EXECUTORS=1
                env.LABELS="<label name>"
                env.USERID='<username>'
                dir('openstack'){
                    sh returnStatus: true, script: """
                    curl -s -k ${env.JENKINS_URL}/jnlpJars/jenkins-cli.jar -o jenkins-cli.jar
                    cat <<-EOF | ${JAVA_HOME}/bin/java -jar ./jenkins-cli.jar -s ${JENKINS_URL} create-node ${NODE_IP}
                    <slave>
                    <name>${NODE_IP}</name>
                    <description>${NODE_IP}</description>
                    <remoteFS>${NODE_SLAVE_HOME}</remoteFS>
                    <numExecutors>${EXECUTORS}</numExecutors>
                    <mode>NORMAL</mode>
                    <retentionStrategy class="hudson.slaves.RetentionStrategy\$Always"/>
                    <launcher class="hudson.slaves.CommandLauncher">
                    <agentCommand>ssh -o StrictHostKeyChecking=no ${USERID}@${NODE_IP} java -jar ${NODE_SLAVE_HOME}/slave.jar</agentCommand>
                    </launcher>
                    <label>${LABELS}</label>
                    <nodeProperties/>
                    </slave>
                    EOF                 
                    """
                }
            }
        }else{
            println("There are no jobs that are waiting for <label name> machines")
            checkAndDestroyOpenstackVM()
        }
    }
}
