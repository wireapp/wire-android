properties([
        parameters([
                credentials(name: 'JENKINSBOT_SECRET', credentialType: 'org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl', defaultValue: 'JENKINSBOT_QA_MAINTENANCE', required: true)
        ]),
        pipelineTriggers([cron('H 01 * * *')])
])

node('built-in') {

    env.ZENDESK_API_URL="https://wearezeta.zendesk.com/api/v2"
    env.ZENDESK_EMAIL="astrid@wire.com"

    env.STAT_PERIOD_DAYS=20
    env.MAX_RECENT_TAGS=10

    sh 'sudo easy_install requests'

    withCredentials([string(credentialsId: "ZENDESK_API_TOKEN", variable: 'ZENDESK_API_TOKEN'), string(credentialsId: params.JENKINSBOT_SECRET, variable: 'JENKINSBOT_SECRET')]) {

        try {

            stage('Get data from Zendesk API') {

                sh '''
rm -f *.csv
'''

                writeFile file: 'script.py', text: '''# -*- coding: utf-8 -*-

import copy
import csv
from datetime import datetime, timedelta
import logging
import httplib
import itertools
from pprint import pformat
import requests
import os
import urllib

import requests.packages.urllib3

requests.packages.urllib3.disable_warnings()

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
channel = logging.StreamHandler()
channel.setLevel(logging.DEBUG)
formatter = logging.Formatter(\'%(asctime)s - %(levelname)s - %(message)s\')
channel.setFormatter(formatter)
logger.addHandler(channel)

# ******************** Zendesk Constants Start *****************************

ZENDESK_EMAIL = os.getenv(\'ZENDESK_EMAIL\')
ZENDESK_API_TOKEN = os.getenv(\'ZENDESK_API_TOKEN\')
ZENDESK_API_URL = os.getenv(\'ZENDESK_API_URL\')

ZENDESK_PLATFORM_IOS = \'iOS\'
ZENDESK_PLATFORM_ANDROID = \'Android\'
ZENDESK_PLATFORM_WINDOWS = \'Windows\'
ZENDESK_PLATFORM_BROWSER_VALUE = \'browser\'
ZENDESK_PLATFORM_OSX_VALUE = \'os_x\'
ZENDESK_PLATFORM_FIELD_NAME = \'Platform\'
ZENDESK_ACCOUNT_TYPE_FIELD_NAME = \'Account type\'
ZENDESK_ACCOUNT_TYPE_TEAM = \'Pro account\'
ZENDESK_AGENT_TRACKING_TAGS_FIELD_NAME = \'Agent Tracking Tags\'
ZENDESK_FEATURE_REQUEST_TYPE_FIELD_NAME = \'Feature Requests\'

STAT_PERIOD_DAYS = int(os.getenv(\'STAT_PERIOD_DAYS\', \'365\'))
STAT_PERIOD_START = datetime.now() - timedelta(days=STAT_PERIOD_DAYS)
DATE_FORMAT = \'%Y-%m-%d\'


# ******************** Zendesk Constants Finish *****************************


class APIError(Exception):
    def __init__(self, message, error_code):
        super(APIError, self).__init__(message)
        self._error_code = error_code

    @property
    def error_code(self):
        return self._error_code


class ZendeskAPI(object):
    # https://developer.zendesk.com/rest_api/docs/help_center/translations#update-translation

    def __init__(self, api_root, login, token):
        self._api_root = api_root
        self._login = login
        self._token = token

    def _get(self, endpoint):
        response = requests.request(\'GET\',
                                    url=\'{}/{}\'.format(self._api_root, endpoint),
                                    headers={\'Content-Type\': \'application/json\',
                                             \'Accept\': \'application/json\'},
                                    auth=(self._login + \'/token\', self._token))
        if response.status_code == httplib.OK:
            return response.json()
        raise APIError(response.text, response.status_code)

    def _post(self, endpoint, post_data=None):
        response = requests.request(\'POST\',
                                    url=\'{}/{}\'.format(self._api_root, endpoint),
                                    headers={\'Content-Type\': \'application/json\',
                                             \'Accept\': \'application/json\'},
                                    auth=(self._login + \'/token\', self._token),
                                    json=post_data)
        if response.status_code in (httplib.OK, httplib.CREATED):
            return response.json()
        raise APIError(response.text, response.status_code)

    def _put(self, endpoint, put_data=None):
        response = requests.request(\'PUT\',
                                    url=\'{}/{}\'.format(self._api_root, endpoint),
                                    headers={\'Content-Type\': \'application/json\',
                                             \'Accept\': \'application/json\'},
                                    auth=(self._login + \'/token\', self._token),
                                    json=put_data)
        if response.status_code in (httplib.OK, httplib.CREATED):
            return response.json()
        raise APIError(response.text, response.status_code)

    def _delete(self, endpoint):
        response = requests.request(\'DELETE\',
                                    url=\'{}/{}\'.format(self._api_root, endpoint),
                                    auth=(self._login + \'/token\', self._token))
        if response.status_code != httplib.NO_CONTENT:
            raise APIError(response.text, response.status_code)

    def search(self, params_dict):
        """https://developer.zendesk.com/rest_api/docs/core/search
        """
        params_str = urllib.urlencode(params_dict)
        result = []
        response = self._get(\'search.json?{}\'.format(params_str))
        result.extend(response[\'results\'])
        page_num = 2
        while response[\'next_page\']:
            updated_params = copy.copy(params_dict)
            updated_params[\'page\'] = page_num
            response = self._get(\'search.json?{}\'.format(urllib.urlencode(updated_params)))
            result.extend(response[\'results\'])
            page_num += 1
        return result

    def list_fields(self):
        return self._get(\'ticket_fields.json\')[\'ticket_fields\']


def search_recent_tickets(zendesk_api, query_str):
    logger.debug(\'query_str in search_recent_tickets: {}\'.format(query_str))
    return zendesk_api.search({\'query\': query_str})


def get_custom_field_names_mapping(zendesk_api):
    all_fields = zendesk_api.list_fields()
    return dict(map(lambda x: (x[\'title\'], x), all_fields))


def get_option_field_values_mapping(field_info, is_custom_field):
    all_options = field_info[\'custom_field_options\' if is_custom_field else \'system_field_options\']
    return dict(map(lambda x: (x[\'name\'], x[\'value\']), all_options))


def get_reverse_option_field_values_mapping(field_info, is_custom_field):
    all_options = field_info[\'custom_field_options\' if is_custom_field else \'system_field_options\']
    return dict(map(lambda x: (x[\'value\'], x[\'name\']), all_options))


def extract_custom_field(ticket, field_id):
    tag = next(itertools.ifilter(lambda x: x[\'id\'] == field_id, ticket[\'custom_fields\']), None)
    return tag[\'value\'] if tag else \'\'


def extract_custom_field_desc(tags_map, tag_name):
    return tags_map[tag_name] if tag_name in tags_map else \'\'


def group_options(actual_mapping):
    group_name_separator = \'|\'
    result = {}
    for name, value in actual_mapping.iteritems():
        group_name = name.split(group_name_separator)[0].strip()
        if group_name in result:
            result[group_name].append(value)
        else:
            result[group_name] = [value]
    return result


def publish_data_as_csv(file_name, headers, raw_data):
    with open(file_name, \'w\') as csvfile:
        writer = csv.writer(csvfile)
        map(lambda x: writer.writerow(x), [headers] + raw_data)


if __name__ == \'__main__\':
    zen_api = ZendeskAPI(ZENDESK_API_URL, ZENDESK_EMAIL, ZENDESK_API_TOKEN)

    field_names_map = get_custom_field_names_mapping(zen_api)
    logger.debug(\'field_names_map: {}\'.format(pformat(field_names_map)))

    platform_name_field = field_names_map[ZENDESK_PLATFORM_FIELD_NAME]
    platform_name_field_options_map = get_option_field_values_mapping(platform_name_field, True)
    logger.debug(\'platform_name_field_options_map: {}\'.format(pformat(platform_name_field_options_map)))

    ticket_tracking_tag_field = field_names_map[ZENDESK_AGENT_TRACKING_TAGS_FIELD_NAME]
    ticket_tracking_tag_field_options_map = get_option_field_values_mapping(ticket_tracking_tag_field, True)
    rev_ticket_tracking_tag_field_options_map = get_reverse_option_field_values_mapping(ticket_tracking_tag_field, True)
    logger.debug(\'ticket_tracking_tag_field_options_map: {}\'.format(pformat(ticket_tracking_tag_field_options_map)))
    ticket_tracking_tag_groups_map = group_options(ticket_tracking_tag_field_options_map)
    logger.debug(\'ticket_tracking_tag_groups_map: {}\'.format(pformat(ticket_tracking_tag_groups_map)))

    ticket_feature_request_field = field_names_map[ZENDESK_FEATURE_REQUEST_TYPE_FIELD_NAME]
    ticket_feature_request_field_options_map = get_option_field_values_mapping(ticket_feature_request_field, True)
    rev_ticket_feature_request_field_options_map = get_reverse_option_field_values_mapping(ticket_feature_request_field,
                                                                                           True)
    logger.debug(\'ticket_feature_request_field_options_map: {}\'.format(
        pformat(ticket_feature_request_field_options_map)))
    ticket_feature_request_groups_map = group_options(ticket_feature_request_field_options_map)
    ticket_tracking_tag_groups_map = group_options(ticket_feature_request_field_options_map)
    logger.debug(\'ticket_feature_request_groups_map: {}\'.format(pformat(ticket_feature_request_groups_map)))

    account_type_field = field_names_map[ZENDESK_ACCOUNT_TYPE_FIELD_NAME]
    account_type_field_options_map = get_option_field_values_mapping(account_type_field, True)
    logger.debug(\'account_type_field_options_map: {}\'.format(pformat(account_type_field_options_map)))
    rev_account_type_field_options_map = get_reverse_option_field_values_mapping(account_type_field, True)

    account_type_field_id = account_type_field[\'id\']
    platform_name_field_id = platform_name_field[\'id\']
    tracking_tag_field_id = ticket_tracking_tag_field[\'id\']
    ticket_feature_request_field_id = ticket_feature_request_field[\'id\']

    all_tickets = search_recent_tickets(zen_api,
                                        \'form:"Report a Bug" \'
                                        \'type:ticket \'
                                        \'created>={} \'.format(STAT_PERIOD_START.strftime(DATE_FORMAT)) +
                                        \'-Tags:"calling_empty_debug_reports"\')
    csv_list = []
    for ticket_item in all_tickets:
        custom_fields = ticket_item.get(\'custom_fields\')
        _created_at = ticket_item.get(\'created_at\')
        _agent_tag = extract_custom_field(ticket_item, tracking_tag_field_id)
        _agent_tag_desc = extract_custom_field_desc(rev_ticket_tracking_tag_field_options_map, _agent_tag)
        _agent_tag_group = _agent_tag_desc.split(\'|\')[0].strip() if _agent_tag_desc.find(\'|\') >= 0 else \'\'

        _platform = extract_custom_field(ticket_item, platform_name_field_id)
        _account_type = extract_custom_field(ticket_item, account_type_field_id)
        _account_type_desc = extract_custom_field_desc(rev_account_type_field_options_map, _account_type)

        csv_list.append([_agent_tag,
                         _agent_tag_desc,
                         _agent_tag_group,
                         _platform,
                         _created_at,
                         str(_account_type_desc == ZENDESK_ACCOUNT_TYPE_TEAM).lower(),
                         ])
    publish_data_as_csv(\'user_issues.csv\',
                        [\'issue_tag\', \'issue_desc\', \'issue_group\', \'platform\', \'created_at\', \'is_team_account\'],
                        csv_list)

    all_tickets = search_recent_tickets(zen_api, \'form:"Feature Request" \'
                                                 \'type:ticket \'
                                                 \'created>={}\'.format(STAT_PERIOD_START.strftime(DATE_FORMAT)))
    csv_list = []
    for ticket_item in all_tickets:
        custom_fields = ticket_item.get(\'custom_fields\')
        _created_at = ticket_item.get(\'created_at\')
        _agent_tag = extract_custom_field(ticket_item, ticket_feature_request_field_id)
        _agent_tag_desc = extract_custom_field_desc(rev_ticket_feature_request_field_options_map, _agent_tag)
        _platform = extract_custom_field(ticket_item, platform_name_field_id)
        _account_type = extract_custom_field(ticket_item, account_type_field_id)
        _account_type_desc = extract_custom_field_desc(rev_account_type_field_options_map, _account_type)

        csv_list.append([_agent_tag_desc,
                         _platform,
                         _created_at,
                         str(_account_type_desc == ZENDESK_ACCOUNT_TYPE_TEAM).lower(),
                         ])
    publish_data_as_csv(\'feature_requests.csv\',
                        [\'feature_desc\', \'platform\', \'created_at\', \'is_team_account\'],
                        csv_list)
'''

                sh 'python script.py'
            }


            archiveArtifacts '*.csv'

            stage('Upload to S3') {
                withAWS(region: 'eu-west-1', credentials: "ANALYTICS_S3_CREDENTIALS") {

                    s3Upload(file: "user_issues.csv", bucket: 'wireanalytics', path: "cs/user_issues.csv", force: true)
                    s3Upload(file: "feature_requests.csv", bucket: 'wireanalytics', path: "cs/feature_requests.csv", force: true)

                }

                wireSend secret: env.JENKINSBOT_SECRET, message: "**Zendesk stats successfully uploaded**"
            }
        } catch (e) {
            print e
            wireSend secret: env.JENKINSBOT_SECRET, message: "**Zendesk stats failed:** " + e.message
            error('Script failed')
        }
    }
}