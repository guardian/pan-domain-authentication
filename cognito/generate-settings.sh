#!/bin/bash
set -e

PRIVATE_KEY_FILE=$(mktemp /tmp/private-key.XXXXXX)
PUBLIC_KEY_FILE=$(mktemp /tmp/public-key.XXXXXX)

function on_exit() {
	rm -f ${PRIVATE_KEY_FILE}
	rm -f ${PUBLIC_KEY_FILE}
}

trap on_exit EXIT

STACK_NAME=$1
REGION=$2
PROFILE=$3

if [ -z ${STACK_NAME} ];
then
    echo "generate-settings.sh STACK_NAME {REGION} {PROFILE}"
    exit 1
fi

if [ -z ${REGION} ];
then
    REGION=eu-west-1
fi

profile_args="--profile ${PROFILE}"

if [ -z ${PROFILE} ];
then
    profile_args=""
fi

user_pool_id=$(aws cloudformation describe-stacks \
    --stack-name ${STACK_NAME} \
    --query 'Stacks[0].Outputs[?OutputKey==`UserPoolId`].OutputValue' \
    --output text \
    --region ${REGION} \
    ${profile_args})

user_pool_client_id=$(aws cloudformation describe-stacks \
    --stack-name ${STACK_NAME} \
    --query 'Stacks[0].Outputs[?OutputKey==`UserPoolClientId`].OutputValue' \
    --output text \
    --region ${REGION} \
    ${profile_args})

settings_bucket=$(aws cloudformation describe-stacks \
    --stack-name ${STACK_NAME} \
    --query 'Stacks[0].Outputs[?OutputKey==`SettingsBucket`].OutputValue' \
    --output text \
    --region ${REGION} \
    ${profile_args})

cookie_name=$(aws cloudformation describe-stacks \
    --stack-name ${STACK_NAME} \
    --query 'Stacks[0].Outputs[?OutputKey==`CookieName`].OutputValue' \
    --output text \
    --region ${REGION} \
    ${profile_args})

cookie_name=$(aws cloudformation describe-stacks \
    --stack-name ${STACK_NAME} \
    --query 'Stacks[0].Outputs[?OutputKey==`CookieName`].OutputValue' \
    --output text \
    --region ${REGION} \
    ${profile_args})

private_settings_file=$(aws cloudformation describe-stacks \
    --stack-name ${STACK_NAME} \
    --query 'Stacks[0].Outputs[?OutputKey==`PrivateSettingsFile`].OutputValue' \
    --output text \
    --region ${REGION} \
    ${profile_args})

public_settings_file=$(aws cloudformation describe-stacks \
    --stack-name ${STACK_NAME} \
    --query 'Stacks[0].Outputs[?OutputKey==`PublicSettingsFile`].OutputValue' \
    --output text \
    --region ${REGION} \
    ${profile_args})

client_id=$(aws cognito-idp describe-user-pool-client \
    --user-pool-id ${user_pool_id} \
    --client-id ${user_pool_client_id} \
    --query 'UserPoolClient.ClientId' \
    --output text \
    --region ${REGION} \
    ${profile_args})

client_secret=$(aws cognito-idp describe-user-pool-client \
    --user-pool-id ${user_pool_id} \
    --client-id ${user_pool_client_id} \
    --query 'UserPoolClient.ClientSecret' \
    --output text \
    --region ${REGION} \
    ${profile_args})

openssl genrsa -out ${PRIVATE_KEY_FILE} 4096
openssl rsa -pubout -in ${PRIVATE_KEY_FILE} -out ${PUBLIC_KEY_FILE}

private_key=`cat ${PRIVATE_KEY_FILE} | sed -e '1d' -e '$d' | tr -d '\n'`
public_key=`cat ${PUBLIC_KEY_FILE} | sed -e '1d' -e '$d' | tr -d '\n'`

private_settings=$(cat <<END
privateKey=${private_key}
publicKey=${public_key}
cookieName=${cookie_name}
clientId=${client_id}
clientSecret=${client_secret}
discoveryDocumentUrl=https://cognito-idp.${REGION}.amazonaws.com/${user_pool_id}/.well-known/openid-configuration
secret=todo-remove-not-used
END
)

public_settings=$(cat <<END
publicKey=${public_key}
END
)

echo "$private_settings" > "/tmp/${private_settings_file}"
echo "$public_settings" > "/tmp/${public_settings_file}"

aws s3 cp "/tmp/${private_settings_file}" s3://${settings_bucket}/${private_settings_file} --region ${REGION} ${profile_args}
aws s3 cp "/tmp/${public_settings_file}" s3://${settings_bucket}/${public_settings_file} --region ${REGION} ${profile_args}