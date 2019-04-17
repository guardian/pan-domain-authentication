#!/bin/bash
set -e

STACK_NAME=$1
EMAIL=$2
REGION=$3
PROFILE=$4

if [ -z ${STACK_NAME} ];
then
    echo "add-user.sh STACK_NAME EMAIL {REGION} {PROFILE}"
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

aws cognito-idp admin-create-user \
    --user-pool-id ${user_pool_id} \
    --username ${EMAIL} \
    --desired-delivery-mediums EMAIL \
    --user-attributes Name=email,Value=${EMAIL} Name=email_verified,Value=True \
    --region ${REGION} \
    ${profile_args}