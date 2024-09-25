import boto3

def get_all_lambda_functions():
    """Retrieve all Lambda functions in the account."""
    client = boto3.client('lambda')
    paginator = client.get_paginator('list_functions')
    functions = []

    for page in paginator.paginate():
        functions.extend(page['Functions'])

    return functions

def get_lambda_versions(function_name):
    """Retrieve all versions of a Lambda function."""
    client = boto3.client('lambda')
    paginator = client.get_paginator('list_versions_by_function')
    versions = []

    for page in paginator.paginate(FunctionName=function_name):
        versions.extend(page['Versions'])

    return versions

def delete_old_lambda_versions(function_name):
    """Delete old versions of the Lambda function, keeping only the latest one."""
    versions = get_lambda_versions(function_name)

    old_versions = sorted([v for v in versions if v['Version'] != '$LATEST'], key=lambda x: int(x['Version']))

    if old_versions:
        latest_version = old_versions.pop()

        for version in old_versions:
            version_number = version['Version']
            print(f"Deleting version {version_number} of Lambda function {function_name}")
            boto3.client('lambda').delete_function(FunctionName=function_name, Qualifier=version_number)

def main():
    functions = get_all_lambda_functions()

    for function in functions:
        function_name = function['FunctionName']
        print(f"Processing Lambda function: {function_name}")
        delete_old_lambda_versions(function_name)

if __name__ == "__main__":
    main()
