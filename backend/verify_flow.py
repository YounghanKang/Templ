import urllib.request
import urllib.parse
import json
import time

base_url = 'http://localhost:8080'

def request(method, path, data=None, headers=None):
    url = base_url + path
    if headers is None: headers = {}
    if data is not None:
        data = json.dumps(data).encode('utf-8')
        headers['Content-Type'] = 'application/json'
    
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as response:
            return response.status, json.loads(response.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8')

# 1. Login to get token
status, data = request('POST', '/api/auth/google', {"accessToken": "dummy123"})
if status != 200:
    print("Login failed", data)
    exit(1)
token = data['accessToken']
headers = {'Authorization': f'Bearer {token}'}

# 2. Create Team
team_data = {
    "name": "FPS Game Team",
    "color": "#ff0000",
    "mission": "fps게임 만들기",
    "slackHandle": "tester"
}
status, team = request('POST', '/api/teams', team_data, headers)
if status != 200:
    print("Team creation failed", team)
    exit(1)
team_id = team['id']
print(f"Created team: {team_id}")

# 3. Submit Spec
spec_data = {
    "author": "tester",
    "specText": "fps게임 만들기"
}
status, spec = request('POST', f'/api/teams/{team_id}/specs', spec_data, headers)
if status != 200:
    print("Spec submission failed", spec)
    exit(1)
spec_id = spec['id']
print(f"Submitted spec: {spec_id}")

# 4. Wait for analysis
print("Waiting for AI analysis...")
time.sleep(2)
while True:
    status, specs = request('GET', f'/api/teams/{team_id}/specs', headers=headers)
    current_spec = next((s for s in specs if s['id'] == spec_id), None)
    if current_spec and current_spec['status'] != 'ANALYZING':
        print(f"Spec status: {current_spec['status']}")
        break
    time.sleep(1)

# 5. Fetch Suggestions
status, suggestions = request('GET', f'/api/teams/{team_id}/specs/{spec_id}/suggestions', headers=headers)
print(f"Got {len(suggestions)} suggestions")

if len(suggestions) > 0:
    root_suggestion = suggestions[-1] # Usually root is created first so it has the smallest ID
    # But wait, in the test it might be different, let's just use suggestions[0] assuming API returns it in some order
    root_suggestion = suggestions[-1]
    print(f"Approving suggestion {root_suggestion['id']} - {root_suggestion['title']}")
    
    # 6. Approve Suggestion
    status, res = request('POST', f'/api/teams/{team_id}/suggestions/{root_suggestion["id"]}/approve', headers=headers)
    if status != 200:
        print("Approval failed", res)
        exit(1)
    print("Approval success")
else:
    print("No suggestions found")
    exit(1)

# 7. Fetch Roadmap Nodes
status, roadmap = request('GET', f'/api/teams/{team_id}/roadmap', headers=headers)
if status != 200:
    print("Failed to fetch roadmap", roadmap)
    exit(1)
nodes = roadmap.get('nodes', [])
edges = roadmap.get('edges', [])
print(f"\nFinal Roadmap Graph:")
print(f"Nodes ({len(nodes)}):")
for n in nodes:
    print(f" - [{n['id']}] {n['label']} (Progress: {n['progress']}%)")
    print(f"   Summary: {n['aiSummary']}")
print(f"Edges ({len(edges)}):")
for e in edges:
    print(f" - {e['from']} -> {e['to']}")
