# https://docs.openstack.org/python-openstackclient/pike/cli/command-objects/server.html#server-list
# installs openstack client as well
export OS_AUTH_URL = "<openstack api url>"
export OS_PROJECT_ID = "<openstach project id>"
export OS_PROJECT_NAME = "<openstack project name>"
export OS_USER_DOMAIN_NAME = "<user domain on openstack>"
export OS_PROJECT_DOMAIN_ID = "<openstack project domain id>"
export OS_USERNAME = "<openstack username>"
export OS_REGION_NAME = "<region name>"
export OS_INTERFACE = "public"
export OS_IDENTITY_API_VERSION = 3

mkdir -p openstack

cd openstack

python3 -m venv venv3
. ./venv3/bin/activate
pip install --upgrade pip
pip install python-openstackclient
openstack --version


openstack keypair create --public-key ~/.ssh/id_rsa.pub my-api-key
