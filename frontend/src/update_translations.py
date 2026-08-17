import sys
import json

def update_json(filepath, updates):
    with open(filepath, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    if 'createTeam' not in data:
        data['createTeam'] = {}
    
    for k, v in updates.items():
        if '.' in k:
            main_key, sub_key = k.split('.')
            if main_key not in data:
                data[main_key] = {}
            data[main_key][sub_key] = v
        else:
            data[k] = v

    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

update_json('locales/en/translation.json', {
    'createTeam.submitButton': 'Create Team',
    'slack_account': 'Slack Account'
})

update_json('locales/ko/translation.json', {
    'createTeam.submitButton': '팀 만들기',
    'slack_account': 'Slack 계정'
})
