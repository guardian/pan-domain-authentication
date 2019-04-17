#!/bin/bash
set -e

STACK_NAME=$1
REGION=$2
PROFILE=$3

if [ -z ${STACK_NAME} ];
then
    echo "configure.sh STACK_NAME {REGION} {PROFILE}"
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

cognito_domain_prefix=$(aws cloudformation describe-stacks \
    --stack-name ${STACK_NAME} \
    --query 'Stacks[0].Outputs[?OutputKey==`CognitoDomainPrefix`].OutputValue' \
    --output text \
    --region ${REGION} \
    ${profile_args})

oauth_callbacks=$(aws cloudformation describe-stacks \
    --stack-name ${STACK_NAME} \
    --query 'Stacks[0].Outputs[?OutputKey==`OAuthCallbacks`].OutputValue' \
    --output text \
    --region ${REGION} \
    ${profile_args})

aws cognito-idp create-user-pool-domain \
    --domain ${cognito_domain_prefix} \
    --user-pool-id ${user_pool_id} \
    --region ${REGION} \
    ${profile_args}

aws cognito-idp update-user-pool-client \
    --user-pool-id ${user_pool_id} \
    --client-id ${user_pool_client_id} \
    --callback-urls ["\"${oauth_callbacks}\""] \
    --allowed-o-auth-flows-user-pool-client \
    --allowed-o-auth-flows code \
    --allowed-o-auth-scopes email openid profile \
    --supported-identity-providers '["COGNITO"]' \
    --region ${REGION} \
    ${profile_args}