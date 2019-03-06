# # installs openstack client as well
export OS_AUTH_URL = "<openstack api url>"
export OS_PROJECT_ID = "<openstach project id>"
export OS_PROJECT_NAME = "<openstack project name>"
export OS_USER_DOMAIN_NAME = "<user domain on openstack>"
export OS_PROJECT_DOMAIN_ID = "<openstack project domain id>"
export OS_USERNAME = "<openstack username>"
export OS_REGION_NAME = "<region name>"
export OS_INTERFACE = "public"
export OS_IDENTITY_API_VERSION = 3

function createOpenstackVM(){
   flavor=$1
   imageId=$2
   keypair=$3
   machineName=$4
   if [[ -z ${machineName} ]]
   then
       machineName='openstack-test-vm'
   fi
   openstack server create --flavor ${flavor} --image ${imageId} --key-name ${keypair} ${machineName}
}

function destroyOpenstackVM(){
    IP=$1
    IMAGE_ID=$(openstack server list --ip "${IP}"|grep "${IP}"|cut -d'|' -f2)
    if [[ -n "${IMAGE_ID}" ]]
    then
       openstack server delete ${IMAGE_ID}
    fi
}

mkdir -p openstack

cd openstack

python3 -m venv venv3
. ./venv3/bin/activate
pip install --upgrade pip
pip install python-openstackclient
openstack --version

createOpenstackVM <flavor> <imageId> <keypair> <machineName> 
destroyOpenstackVM <ip>


