"""
미궁 맵 생성 + 검증 (관리자 웹 JSON 직접 편집에 붙여 넣을 map_data).

실행: python docs/maps/build_labyrinth.py 25   (결과: docs/maps/labyrinth-25.json)
      python docs/maps/build_labyrinth.py 50   (결과: docs/maps/labyrinth-50.json)

구성 (4구역을 시계 방향으로 지나 출구로):
  1구역 (왼쪽 위, 시작)   고리가 있는 미로. 가장 먼 막다른 곳의 빨간 열쇠 -> 빨간 문으로 2구역
  2구역 (오른쪽 위)       미로 끝의 상자 방. 상자를 밀어 길을 내야 아래로 나갈 수 있다
  3구역 (오른쪽 아래)     갈림 없는 미로. 방화벽(3번) -> 감염된 컴퓨터를 복구하면 레이저 문이 열림 -> 깊은 곳의 엔티티가 파란 열쇠
  4구역 (왼쪽 아래)       파란 문으로 들어가 고리 미로 끝의 출구
  + 각 구역 입구 체크포인트, 벽처럼 보이는 가짜 벽 안쪽 비밀 칸의 데이터 파편

검증: 게임 Board 규칙(밀기·부수기·열쇠·문·체크포인트·가짜 벽·컴퓨터 3x3 해킹·인물 대화)을 그대로 옮긴 BFS로
출구까지 갈 수 있는지, 상자를 밀지 않고는 상자 방을 지나갈 수 없는지 확인한다.
"""
import json
import random
import sys
from collections import deque
from pathlib import Path

sys.stdout.reconfigure(encoding="utf-8")
N = int(sys.argv[1]) if len(sys.argv) > 1 else 25
OUT = Path(__file__).resolve().parent / f"labyrinth-{N}.json"
SEED = 20261125

# 크기별 설정. 상자 방(C 상자, # 벽)은 왼쪽 위 칸으로 들어와 오른쪽 아래 칸에서 아래 벽으로 나간다
SIZES = {
    25: dict(room=(17, 7), turns=200, pc_words=5, pc_time=40, guide="미궁입니다. M을 누르면 맵 전체를 볼 수 있습니다.",
             room_rows=[  # 7 x 5, 최소 6번 밀어야 한다
                 ".C.....",
                 "C#..#C.",
                 "..#.#..",
                 "...#.C.",
                 "..###..",
             ]),
    50: dict(room=(35, 15), turns=500, pc_words=6, pc_time=40, guide="거대한 미궁입니다. M을 누르면 맵 전체를 볼 수 있습니다.",
             room_rows=[  # 13 x 9, 최소 7번 밀어야 한다
                 ".##...C......",
                 "C..#..##.....",
                 ".....#.C..#.#",
                 ".....#.#..###",
                 ".#.#.C#C#....",
                 "...##..#..C..",
                 "#..##.#..#C#.",
                 ".....#.#.#..#",
                 "..#C..#.##...",
             ]),
}
assert N in SIZES, f"크기는 {sorted(SIZES)} 중 하나"
CFG = SIZES[N]
D = (N - 1) // 2  # 구역 사이 벽 줄 (50 -> 24, 25 -> 12)
A1, A2 = 1, D - 1          # 왼쪽·위 구역 칸 범위
B1, B2 = D + 1, 2 * D - 1  # 오른쪽·아래 구역 칸 범위
TURNS = CFG["turns"]

grid = [["#"] * N for _ in range(N)]
objects = []


def obj(**o):
    objects.append(o)
    return o


def cells(x0, x1, y0, y1):
    return [(x, y) for y in range(y0, y1 + 1, 2) for x in range(x0, x1 + 1, 2)]


def carve_maze(rng, x0, x1, y0, y1, start, exclude=frozenset(), loops=0.0):
    """홀수 좌표 칸으로 이루어진 미로 (재귀 백트래킹). loops: 칸 사이 벽을 이 비율만큼 더 뚫어 고리를 만든다"""
    all_cells = [c for c in cells(x0, x1, y0, y1) if c not in exclude]
    cellset = set(all_cells)
    for x, y in all_cells:
        grid[y][x] = "."
    seen = {start}
    stack = [start]
    while stack:
        x, y = stack[-1]
        nbrs = [(x + dx, y + dy) for dx, dy in ((2, 0), (-2, 0), (0, 2), (0, -2)) if (x + dx, y + dy) in cellset and (x + dx, y + dy) not in seen]
        if not nbrs:
            stack.pop()
            continue
        nx, ny = rng.choice(nbrs)
        grid[(y + ny) // 2][(x + nx) // 2] = "."
        seen.add((nx, ny))
        stack.append((nx, ny))
    assert seen == cellset, "미로가 다 이어지지 않음"
    if loops:
        walls = [((x + nx) // 2, (y + ny) // 2) for (x, y) in all_cells for (nx, ny) in ((x + 2, y), (x, y + 2))
                 if (nx, ny) in cellset and grid[(y + ny) // 2][(x + nx) // 2] == "#"]
        for wx, wy in rng.sample(walls, int(len(walls) * loops)):
            grid[wy][wx] = "."
    return all_cells


def floor_bfs(start, blocked=frozenset()):
    dist = {start: 0}
    q = deque([start])
    while q:
        x, y = q.popleft()
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            p = (x + dx, y + dy)
            if 0 <= p[0] < N and 0 <= p[1] < N and grid[p[1]][p[0]] == "." and p not in dist and p not in blocked:
                dist[p] = dist[(x, y)] + 1
                q.append(p)
    return dist


def path_to(start, goal):
    prev = {start: None}
    q = deque([start])
    while q:
        c = q.popleft()
        if c == goal:
            break
        x, y = c
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            p = (x + dx, y + dy)
            if grid[p[1]][p[0]] == "." and p not in prev:
                prev[p] = c
                q.append(p)
    out, c = [], goal
    while c is not None:
        out.append(c)
        c = prev[c]
    return out[::-1]


def dead_ends(cell_list):
    return [(x, y) for (x, y) in cell_list
            if sum(grid[y + dy][x + dx] == "." for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))) == 1]


def secret_pocket(rng, cell_list, used, name):
    """막다른 칸 하나를 사방 벽으로 막고, 벽 하나를 가짜 벽으로 바꿔 데이터 파편을 숨긴다"""
    for x, y in rng.sample(dead_ends(cell_list), len(dead_ends(cell_list))):
        if (x, y) in used:
            continue
        opening = [(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if grid[y + dy][x + dx] == "."][0]
        used.add((x, y))
        obj(id=f"shard_{name}", type="hidden", x=x, y=y, name="버그 조각")
        obj(id=f"fake_{name}", type="fakeWall", x=opening[0], y=opening[1])
        return


rng = random.Random(SEED)
used = set()

# ---------------- 1구역: 왼쪽 위 (시작) ----------------
start = (1, 1)
q1 = carve_maze(rng, A1, A2, A1, A2, start, loops=0.08)
d1 = floor_bfs(start)
red_key = max(dead_ends(q1), key=lambda c: d1[c])
used |= {start, red_key}
obj(id="key_red", type="key", x=red_key[0], y=red_key[1], keyId="red")
# 빨간 문: 오른쪽 벽(x=D)에서 시작점과 열쇠 둘 다에서 먼 줄
door1_y = max(range(A1, A2 + 1, 2), key=lambda y: d1[(A2, y)] + abs(y - red_key[1]))
grid[door1_y][D] = "."
obj(id="door_red", type="door", x=D, y=door1_y, keyId="red")
secret_pocket(rng, q1, used | {(A2, door1_y)}, "q1")

# ---------------- 2구역: 오른쪽 위 (상자 방) ----------------
ROOM_X, ROOM_Y = CFG["room"]
ROOM = CFG["room_rows"]
assert (ROOM_X + len(ROOM[0]) - 1, ROOM_Y + len(ROOM) - 1) == (B2, A2), "상자 방은 2구역 오른쪽 아래 구석에 붙어야 한다"
room_cells = {(ROOM_X + x, ROOM_Y + y) for y in range(len(ROOM)) for x in range(len(ROOM[0]))}
q2_entry = (B1, door1_y)
q2 = carve_maze(rng, B1, B2, A1, A2, q2_entry, exclude=frozenset(room_cells), loops=0.05)
for y, row in enumerate(ROOM):
    for x, ch in enumerate(row):
        gx, gy = ROOM_X + x, ROOM_Y + y
        grid[gy][gx] = "#" if ch == "#" else "."
        if ch == "C":
            obj(id=f"crate{len([o for o in objects if o['type'] == 'crate']) + 1}", type="crate", x=gx, y=gy)
# 방 위쪽 입구 하나 (미로 -> 방 왼쪽 위), 바깥 둘레는 막는다
for x in range(ROOM_X - 1, ROOM_X + len(ROOM[0]) + 1):
    if 0 < x < N - 1:
        grid[ROOM_Y - 1][x] = "#"
for y in range(ROOM_Y, ROOM_Y + len(ROOM)):
    grid[y][ROOM_X - 1] = "#"
grid[ROOM_Y - 1][ROOM_X] = "."
grid[D][B2] = "."  # 방 -> 3구역
obj(id="cp1", type="checkpoint", x=q2_entry[0], y=q2_entry[1], order=1, turns=TURNS)
used.add(q2_entry)

# ---------------- 3구역: 오른쪽 아래 (컴퓨터·레이저·엔티티) ----------------
q3_entry = (B2, B1)
q3 = carve_maze(rng, B1, B2, B1, B2, q3_entry)
obj(id="cp2", type="checkpoint", x=q3_entry[0], y=q3_entry[1], order=2, turns=TURNS)
used.add(q3_entry)
d3 = floor_bfs(q3_entry)
npc_cell = max(dead_ends(q3), key=lambda c: d3[c])
route = path_to(q3_entry, npc_cell)
firewall = route[int(len(route) * 0.2)]
# 레이저: 엔티티로 가는 길 중간쯤. 컴퓨터: 레이저보다 앞쪽에서 닿는 막다른 칸 중 입구에서 먼 곳
# (작은 맵에서 그런 칸이 없으면 레이저를 조금씩 뒤로 민다)
for frac in (0.55, 0.65, 0.75, 0.85):
    laser = route[int(len(route) * frac)]
    before_laser = floor_bfs(q3_entry, blocked=frozenset({laser}))
    pc_options = [c for c in dead_ends(q3) if c in before_laser and c not in route and c != q3_entry]
    if pc_options:
        break
assert pc_options, "컴퓨터를 둘 막다른 칸이 없음"
pc_cell = max(pc_options, key=lambda c: before_laser[c])
obj(id="laser", type="door", x=laser[0], y=laser[1], keyId="laser")
obj(id="fw3", type="breakable", x=firewall[0], y=firewall[1], hp=3)
obj(id="pc", type="computer", x=pc_cell[0], y=pc_cell[1], name="감염된 보안 PC", wordCount=CFG["pc_words"], timeLimit=CFG["pc_time"],
    effects=[{"type": "remove", "target": "laser"}])
obj(id="entity", type="npc", x=npc_cell[0], y=npc_cell[1], name="엔티티", dialogueId="entity")
used |= {laser, firewall, pc_cell, npc_cell}
# 파란 문: 3구역 왼쪽 벽(x=D). 엔티티에서 먼 줄
door2_y = max((y for y in range(B1, B2 + 1, 2) if (B1, y) not in used), key=lambda y:abs(y - npc_cell[1]) + d3.get((B1, y), 0))
grid[door2_y][D] = "."
obj(id="door_blue", type="door", x=D, y=door2_y, keyId="blue")

# ---------------- 4구역: 왼쪽 아래 (출구) ----------------
q4_entry = (A2, door2_y)
q4 = carve_maze(rng, A1, A2, B1, B2, q4_entry, loops=0.08)
obj(id="cp3", type="checkpoint", x=q4_entry[0], y=q4_entry[1], order=3, turns=TURNS)
used.add(q4_entry)
d4 = floor_bfs(q4_entry)
exit_cell = max(dead_ends(q4), key=lambda c: d4[c])
obj(id="exit", type="exit", x=exit_cell[0], y=exit_cell[1])
used.add(exit_cell)
r4 = path_to(q4_entry, exit_cell)
fw2 = r4[len(r4) // 2]
obj(id="fw2", type="breakable", x=fw2[0], y=fw2[1], hp=2)
used.add(fw2)
secret_pocket(rng, q4, used, "q4")

player = {"x": start[0], "y": start[1]}

DIALOGUES = {
    "entity": {
        "start": "e1",
        "nodes": {
            "e1": {"speaker": "엔티티", "text": "……여기까지 온 집행자는 처음이군.", "next": "e2"},
            "e2": {"speaker": "엔티티", "text": "이 미궁은 버려진 코드의 잔해다. 길은 계속 엉키고, 문은 아무에게나 열리지 않지.",
                   "choices": [{"text": "출구는 어디야?", "next": "e3"}, {"text": "넌 누구지?", "next": "e4"}]},
            "e3": {"speaker": "엔티티", "text": "서쪽. 파란 문 너머다. 이 열쇠를 가져가라.", "next": "e5"},
            "e4": {"speaker": "엔티티", "text": "이름 같은 건 없다. 그저 지켜보는 자… 그래도 너를 돕는 건 손해가 아니겠지.", "next": "e3"},
            "e5": {"speaker": "엔티티", "text": "……길을 잃지 마라.", "effects": [{"type": "giveKey", "keyId": "blue"}]},
        },
    }
}

GUIDES = [
    {"on": "start", "text": CFG["guide"]},
    {"on": "start", "text": "빨간 열쇠를 찾아 동쪽의 빨간 문을 여십시오."},
]

map_data = {
    "version": 1,
    "theme": "undercity",
    "width": N,
    "height": N,
    "tiles": ["".join(r) for r in grid],
    "player": player,
    "objects": objects,
    "dialogues": DIALOGUES,
    "guides": GUIDES,
    "camera": {"width": 17, "height": 11},
    "minimap": {"revealRadius": 3},
}


# ---------------- 검증 (게임 Board 규칙) ----------------
def solve(md, allow_push=True):
    tiles = md["tiles"]
    objs = md["objects"]
    by_id = {o["id"]: i for i, o in enumerate(objs)}

    def wall(x, y):
        return not (0 <= x < N and 0 <= y < N) or tiles[y][x] == "#"

    crates0 = tuple(sorted((o["x"], o["y"]) for o in objs if o["type"] == "crate"))
    static = {(o["x"], o["y"]): i for i, o in enumerate(objs) if o["type"] != "crate"}
    hp0 = tuple(o.get("hp", 0) if o["type"] == "breakable" else 0 for o in objs)
    # 상태: (pos, crates, removed bitmask, hp tuple, keys tuple(sorted (id,count)), talked)
    s0 = ((player["x"], player["y"]), crates0, 0, hp0, (), False)
    seen = {s0}
    q = deque([s0])
    while q:
        pos, crates, removed, hp, keys, talked = q.popleft()
        kd = dict(keys)
        cset = set(crates)

        def obj_at(p):
            i = static.get(p)
            if i is not None and not (removed >> i) & 1:
                return i
            return "crate" if p in cset else None

        nexts = []
        # 이동
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            to = (pos[0] + dx, pos[1] + dy)
            if wall(*to):
                continue
            t = obj_at(to)
            if t is None:
                nexts.append((to, crates, removed, hp, keys, talked))
                continue
            if t == "crate":
                if not allow_push:
                    continue
                beyond = (to[0] + dx, to[1] + dy)
                if wall(*beyond) or obj_at(beyond) is not None:
                    continue
                nc = tuple(sorted((beyond if c == to else c) for c in crates))
                nexts.append((pos, nc, removed, hp, keys, talked))
                continue
            o = objs[t]
            ty = o["type"]
            if ty in ("npc", "anomaly", "computer"):
                continue
            if ty == "exit":
                return True
            if ty == "breakable":
                h = list(hp)
                h[t] -= 1
                nr = removed | (1 << t) if h[t] <= 0 else removed
                nexts.append((pos, crates, nr, tuple(h), keys, talked))
                continue
            if ty == "door":
                if kd.get(o["keyId"], 0) <= 0:
                    continue
                k2 = dict(kd)
                k2[o["keyId"]] -= 1
                nexts.append((to, crates, removed | (1 << t), hp, tuple(sorted(k2.items())), talked))
                continue
            if ty == "key":
                k2 = dict(kd)
                k2[o["keyId"]] = k2.get(o["keyId"], 0) + 1
                nexts.append((to, crates, removed | (1 << t), hp, tuple(sorted(k2.items())), talked))
                continue
            nexts.append((to, crates, removed, hp, keys, talked))  # 체크포인트·가짜 벽·파편: 지나간다 (파편 수집은 길과 상관없음)
        # 3x3 안의 컴퓨터 해킹 / 인물 대화
        for i, o in enumerate(objs):
            if (removed >> i) & 1 or max(abs(o["x"] - pos[0]), abs(o["y"] - pos[1])) > 1:
                continue
            if o["type"] == "computer":
                nr = removed | (1 << i)  # 복구 (다시 해킹하지 않음)
                for e in o.get("effects", []):
                    if e["type"] == "remove":
                        nr |= 1 << by_id[e["target"]]
                nexts.append((pos, crates, nr, hp, keys, talked))
            if o["type"] == "npc" and not talked:
                k2 = dict(kd)
                for node in md["dialogues"][o["dialogueId"]]["nodes"].values():
                    for e in node.get("effects", []):
                        if e["type"] == "giveKey":
                            k2[e["keyId"]] = k2.get(e["keyId"], 0) + 1
                nexts.append((pos, crates, removed, hp, tuple(sorted(k2.items())), True))
        for s in nexts:
            if s[0][1] >= B1:  # 상자 방을 지나 아래 구역으로 오면 상자 위치는 더 이상 길에 영향이 없다
                s = (s[0], (), s[2], s[3], s[4], s[5])
            if s not in seen:
                seen.add(s)
                q.append(s)
    return False


# 상자 방을 상자 없이 지나갈 수 없는지 (방 입구 -> 출구 줄)
room_entry, room_exit = (ROOM_X, ROOM_Y), (B2, A2)
crate_cells = {(o["x"], o["y"]) for o in objects if o["type"] == "crate"}
assert room_exit not in floor_bfs(room_entry, blocked=frozenset(crate_cells)), "상자 방이 상자를 밀지 않아도 지나가짐"
assert solve(map_data), "클리어할 수 없음"
assert not solve(map_data, allow_push=False), "상자를 밀지 않아도 클리어됨"

floor = sum(r.count(".") for r in map_data["tiles"])
shortest = floor_bfs(start)
print(f"OK {N}x{N} · 바닥 {floor}칸 · 오브젝트 {len(objects)}개 "
      f"(상자 {len(crate_cells)}, 문 3, 방화벽 2, 체크포인트 3, 컴퓨터 1, 인물 1, 파편 2, 가짜 벽 2)")
OUT.write_text(json.dumps(map_data, ensure_ascii=False, indent=2), encoding="utf-8")
print("저장:", OUT)
print("\n".join(map_data["tiles"]))
