"""
튜토리얼 맵 생성 + 검증.
ASCII 그림으로 맵을 정의하고, map_data JSON(관리자 등록 형식)으로 저장한다.
저장 전에 스키마 검사와 퍼즐 풀이(BFS)로 클리어 가능 여부와 최소 턴을 확인한다.

실행: python docs/maps/build_tutorial.py  (결과: docs/maps/tutorial/stage-N.json)
규칙과 필드 설명은 docs/map-data.md 참고. 맵을 고치면 이 파일의 STAGES를 고치고 다시 실행한다.

그림 기호: # 벽, . 바닥, P 시작, E 출구, C 밀 수 있는 상자, B 부술 수 있는 블록, K 열쇠, D 잠긴 문, N 인물
"""
import heapq
import json
import sys

sys.stdout.reconfigure(encoding="utf-8")
from pathlib import Path

OUT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent / "tutorial"

NARRATION = [
    "당신은 과거의 코드들을 정리하던 도중 알 수 없는 오류를 발견하였습니다.",
    "그와 동시에 모니터 화면은 불길하게 물들고, 이후 정신을 잃었습니다.",
    "눈을 뜬 당신은 알 수 없는 세계에서 눈을 뜹니다.",
    "이 세계의 법칙을 뒤틀어 특별한 능력을 사용할 수 있는 당신을 이곳에선 세계 밖에서 온 집행자라고 부릅니다.",
    "당신은 원래 세계로 돌아가기 위해 이곳을 정상화시켜야 합니다.",
    "그렇기에 디지털 세계의 시스템을 복구하고, 오류를 처단하는 집행자가 되었습니다.",
    "집행자로서의 당신은 지금부터 여러 도시에 발령되어, 그곳에서의 문제를 해결하게 될 것입니다.",
    "당신의 여정 속 모든 프로세스가 정상 종료되기를.",
]

STAGES = [
    {
        "stageNo": 1, "title": "시뮬레이션 1: 이동", "rewardCoin": 10,
        "grid": [
            "#########",
            "#P..#...#",
            "###.#.#.#",
            "#...#.#.#",
            "#.###.#.#",
            "#.....#E#",
            "#########",
        ],
        "story": {
            "intro": [{"type": "narration", "text": t} for t in NARRATION]
                     + [{"type": "inputCharacterName", "text": "집행자의 이름을 입력하십시오."}],
        },
        "guides": [
            {"on": "start", "text": "W, A, S, D 키를 눌러 이동할 수 있습니다. 한 번 움직일 때마다 1턴이 지납니다."},
            {"on": "start", "text": "반짝이는 출구까지 이동하십시오."},
        ],
    },
    {
        "stageNo": 2, "title": "시뮬레이션 2: 장애물", "rewardCoin": 10,
        "grid": [
            "#########",
            "#P#######",
            "#C#######",
            "#....B.E#",
            "#.#######",
            "#########",
        ],
        "breakableHp": 2,
        "guides": [
            {"on": "start", "text": "앞에 장애물이 있을 때 그 방향으로 이동하면, 턴을 소모해서 장애물을 밀거나 부술 수 있습니다."},
            {"on": "near", "target": "crate1", "text": "상자는 밀 수 있습니다. 상자를 아래쪽 빈칸까지 밀어 길을 여십시오."},
            {"on": "near", "target": "block1", "text": "금이 간 블록은 부술 수 있습니다. 여러 번 부딪혀야 하는 블록도 있습니다."},
        ],
    },
    {
        "stageNo": 3, "title": "시뮬레이션 3: 열쇠", "rewardCoin": 10,
        "grid": [
            "#########",
            "#P..D..E#",
            "#.#######",
            "#...K####",
            "#########",
        ],
        "guides": [
            {"on": "start", "text": "잠긴 문은 열쇠가 있어야 열 수 있습니다."},
            {"on": "pickup", "target": "key1", "text": "열쇠를 얻었습니다. 문으로 가면 열쇠를 써서 문을 엽니다."},
        ],
    },
    {
        "stageNo": 4, "title": "시뮬레이션 4: 인물", "rewardCoin": 10,
        "grid": [
            "###########",
            "#P....#...#",
            "#.....D.E.#",
            "#..N..#...#",
            "###########",
        ],
        "npcDialogue": "researcher",
        "guides": [
            {"on": "start", "text": "인물 주변 3x3 칸 안에 들어가면 E 키로 대화를 걸 수 있습니다. 대화는 턴을 소모하지 않습니다."},
        ],
        "dialogues": {
            "researcher": {
                "start": "n1",
                "nodes": {
                    "n1": {"speaker": "연구원", "text": "아, 깨어났군요. {characterName} 집행자님, 맞으시죠?", "next": "n2"},
                    "n2": {"speaker": "연구원", "text": "이곳은 집행자 훈련용 홀로그램 시뮬레이션입니다. 무엇이 궁금하신가요?",
                           "choices": [
                               {"text": "여기서 나가려면 어떻게 해야 하죠?", "next": "n3"},
                               {"text": "집행자가 뭐죠?", "next": "n4"},
                           ]},
                    "n3": {"speaker": "연구원", "text": "저 문 너머가 출구예요. 이 열쇠를 가져가세요.",
                           "effects": [{"type": "giveKey", "keyId": "lab"}]},
                    "n4": {"speaker": "연구원", "text": "세계의 법칙을 뒤틀어 오류를 처단하는 사람이죠. 일단은 이 열쇠로 문부터 열어 보세요.",
                           "effects": [{"type": "giveKey", "keyId": "lab"}]},
                },
            }
        },
    },
    {
        "stageNo": 5, "title": "시뮬레이션 5: 종료", "rewardCoin": 10,
        "grid": [
            "###########",
            "#P..#.....#",
            "#.#.#.###.#",
            "#.#...#...#",
            "#.#####.###",
            "#...#...#E#",
            "###.#.#.#.#",
            "#.....#...#",
            "###########",
        ],
        "guides": [
            {"on": "start", "text": "문 아이콘이 있는 출구에 도착하면 스테이지를 클리어합니다."},
            {"on": "start", "text": "막다른 길도 있습니다. 출구까지 가는 길을 찾으십시오."},
        ],
    },
    {
        "stageNo": 6, "title": "시뮬레이션 6: 종합", "rewardCoin": 50,
        "grid": [
            "#############",
            "#P..C.#######",
            "####.########",
            "####.....B..#",
            "###########.#",
            "#E.D.....N..#",
            "#####.......#",
            "#############",
        ],
        "breakableHp": 2,
        "npcDialogue": "supervisor",
        "dialogues": {
            "supervisor": {
                "start": "n1",
                "nodes": {
                    "n1": {"speaker": "감독관", "text": "{characterName} 집행자님, 훈련은 여기까지입니다.", "next": "n2"},
                    "n2": {"speaker": "감독관", "text": "이 열쇠로 문을 열고 시뮬레이션을 종료하십시오.", "next": "n3",
                           "effects": [{"type": "giveKey", "keyId": "lab"}]},
                    "n3": {"speaker": "감독관", "text": "실전에서도 모든 프로세스가 정상 종료되기를."},
                },
            }
        },
        "story": {
            "outro": [
                {"type": "narration", "text": "홀로그램 시뮬레이션이 종료되었습니다."},
                {"type": "narration", "text": "{characterName} 집행자, 첫 발령지로 이동합니다."},
            ],
        },
        "guides": [
            {"on": "start", "text": "지금까지 배운 이동, 장애물, 열쇠, 대화를 모두 사용해 홀로그램 시뮬레이션을 탈출하십시오."},
        ],
    },
]

DIRS = {"W": (0, -1), "A": (-1, 0), "S": (0, 1), "D": (1, 0)}


def build(stage):
    grid = stage["grid"]
    h, w = len(grid), len(grid[0])
    assert all(len(r) == w for r in grid), f"stage {stage['stageNo']}: 줄 길이가 다름"
    tiles, objects, player, counters = [], [], None, {}

    def next_id(kind):
        counters[kind] = counters.get(kind, 0) + 1
        return f"{kind}{counters[kind]}"

    for y, row in enumerate(grid):
        tiles.append("".join("#" if c == "#" else "." for c in row))
        for x, c in enumerate(row):
            if c == "P":
                player = {"x": x, "y": y}
            elif c == "E":
                objects.append({"id": next_id("exit"), "type": "exit", "x": x, "y": y})
            elif c == "C":
                objects.append({"id": next_id("crate"), "type": "crate", "x": x, "y": y})
            elif c == "B":
                objects.append({"id": next_id("block"), "type": "breakable", "x": x, "y": y,
                                "hp": stage.get("breakableHp", 1)})
            elif c == "K":
                objects.append({"id": next_id("key"), "type": "key", "x": x, "y": y, "keyId": "lab"})
            elif c == "D":
                objects.append({"id": next_id("door"), "type": "door", "x": x, "y": y, "keyId": "lab"})
            elif c == "N":
                objects.append({"id": next_id("npc"), "type": "npc", "x": x, "y": y,
                                "name": "연구원" if stage.get("npcDialogue") == "researcher" else "감독관",
                                "dialogueId": stage["npcDialogue"]})
            elif c not in "#.":
                raise ValueError(f"모르는 기호 {c!r}")

    map_data = {
        "version": 1,
        "theme": "hologram",
        "width": w,
        "height": h,
        "tiles": tiles,
        "player": player,
        "objects": objects,
    }
    for key in ("dialogues", "guides", "story"):
        if key in stage:
            map_data[key] = stage[key]
    map_data["hints"] = []
    return map_data


def validate(md, no):
    w, h, tiles = md["width"], md["height"], md["tiles"]
    assert len(tiles) == h and all(len(r) == w for r in tiles)
    for x in range(w):
        assert tiles[0][x] == "#" and tiles[h - 1][x] == "#", f"{no}: 위아래 테두리"
    for y in range(h):
        assert tiles[y][0] == "#" and tiles[y][w - 1] == "#", f"{no}: 좌우 테두리"
    ids, cells = set(), set()
    for o in md["objects"] + [dict(md["player"], id="player", type="player")]:
        assert o["id"] not in ids, f"{no}: id 중복 {o['id']}"
        ids.add(o["id"])
        assert tiles[o["y"]][o["x"]] == ".", f"{no}: {o['id']}가 벽 위에 있음"
        assert (o["x"], o["y"]) not in cells, f"{no}: 같은 칸에 오브젝트 두 개"
        cells.add((o["x"], o["y"]))
    assert sum(o["type"] == "exit" for o in md["objects"]) == 1, f"{no}: 출구는 하나"
    dialogues = md.get("dialogues", {})
    for o in md["objects"]:
        if o["type"] == "npc":
            d = dialogues[o["dialogueId"]]
            assert d["start"] in d["nodes"]
            for node in d["nodes"].values():
                for nxt in [node.get("next")] + [c["next"] for c in node.get("choices", [])]:
                    assert nxt is None or nxt in d["nodes"], f"{no}: 없는 대화 노드 {nxt}"
    for g in md.get("guides", []):
        assert g["on"] in ("start", "near", "pickup")
        if g["on"] != "start":
            assert g["target"] in ids, f"{no}: 안내 대상 {g['target']} 없음"


def solve(md):
    """최소 턴 BFS. 대화는 0턴 (Dijkstra). 규칙은 docs/map-data.md의 이동 규칙과 같다."""
    tiles = md["tiles"]
    objs = md["objects"]
    exit_cell = next((o["x"], o["y"]) for o in objs if o["type"] == "exit")
    npcs = {(o["x"], o["y"]): o for o in objs if o["type"] == "npc"}
    gives = {}
    for cell, npc in npcs.items():
        d = md["dialogues"][npc["dialogueId"]]
        keys = sorted({e["keyId"] for n in d["nodes"].values() for e in n.get("effects", []) if e["type"] == "giveKey"})
        gives[cell] = keys
    start = (
        md["player"]["x"], md["player"]["y"],
        frozenset((o["x"], o["y"]) for o in objs if o["type"] == "crate"),
        frozenset(((o["x"], o["y"]), o["hp"]) for o in objs if o["type"] == "breakable"),
        frozenset((o["x"], o["y"]) for o in objs if o["type"] == "key"),   # 바닥에 남은 열쇠
        frozenset((o["x"], o["y"]) for o in objs if o["type"] == "door"),  # 잠긴 문
        0,                # 가진 열쇠 수
        frozenset(),      # 대화한 인물
    )
    dist = {start: 0}
    prev = {start: None}
    pq = [(0, 0, start)]
    tie = 0
    while pq:
        d, _, s = heapq.heappop(pq)
        if d > dist[s]:
            continue
        px, py, crates, blocks, keys, doors, held, talked = s
        if (px, py) == exit_cell:
            path, cur = [], s
            while prev[cur]:
                cur, act = prev[cur]
                path.append(act)
            return d, "".join(reversed(path))
        block_hp = dict(blocks)
        nexts = []
        # 대화 (0턴): 3x3 안의 인물, 처음 한 번만 열쇠를 받는다
        for (nx, ny), keys_given in gives.items():
            if max(abs(nx - px), abs(ny - py)) <= 1 and (nx, ny) not in talked:
                nexts.append((0, "T", (px, py, crates, blocks, keys, doors, held + len(keys_given), talked | {(nx, ny)})))
        for name, (dx, dy) in DIRS.items():
            tx, ty = px + dx, py + dy
            if tiles[ty][tx] == "#" or (tx, ty) in npcs:
                continue  # 벽·인물: 입력 무시 (턴 없음)
            if (tx, ty) in crates:
                bx, by = tx + dx, ty + dy
                occupied = (tiles[by][bx] == "#" or (bx, by) in crates or (bx, by) in block_hp
                            or (bx, by) in keys or (bx, by) in doors or (bx, by) in npcs or (bx, by) == exit_cell)
                if occupied:
                    continue  # 밀 수 없음: 입력 무시
                nexts.append((1, name, (px, py, (crates - {(tx, ty)}) | {(bx, by)}, blocks, keys, doors, held, talked)))
                continue
            if (tx, ty) in block_hp:
                hp = block_hp[(tx, ty)] - 1
                nb = {c: v for c, v in block_hp.items() if c != (tx, ty)}
                if hp > 0:
                    nb[(tx, ty)] = hp
                nexts.append((1, name, (px, py, crates, frozenset(nb.items()), keys, doors, held, talked)))
                continue
            if (tx, ty) in doors:
                if held == 0:
                    continue  # 열쇠 없음: 입력 무시
                nexts.append((1, name, (tx, ty, crates, blocks, keys, doors - {(tx, ty)}, held - 1, talked)))
                continue
            if (tx, ty) in keys:
                nexts.append((1, name, (tx, ty, crates, blocks, keys - {(tx, ty)}, doors, held + 1, talked)))
                continue
            nexts.append((1, name, (tx, ty, crates, blocks, keys, doors, held, talked)))
        for cost, act, ns in nexts:
            nd = d + cost
            if nd < dist.get(ns, 1 << 30):
                dist[ns] = nd
                prev[ns] = (s, act)
                tie += 1
                heapq.heappush(pq, (nd, tie, ns))
    return None, None


OUT.mkdir(parents=True, exist_ok=True)
for stage in STAGES:
    md = build(stage)
    validate(md, stage["stageNo"])
    moves, path = solve(md)
    assert moves is not None, f"stage {stage['stageNo']}: 클리어할 수 없음"
    body = {
        "stageNo": stage["stageNo"],
        "stageType": "TUTORIAL",
        "title": stage["title"],
        "parMoves": moves,
        "rewardCoin": stage["rewardCoin"],
        "mapData": md,
    }
    path_file = OUT / f"stage-{stage['stageNo']}.json"
    path_file.write_text(json.dumps(body, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    size = len(json.dumps(md, ensure_ascii=False).encode("utf-8"))
    print(f"stage {stage['stageNo']} {stage['title']}: {md['width']}x{md['height']}, 최소 {moves}턴, 풀이 {path}, {size}B")
    for row in stage["grid"]:
        print("   ", row)
