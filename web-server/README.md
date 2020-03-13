#### 创建一个地图

创建一个地图分三个步骤

##### 1. 新建一个地图

```
/map POST
Content-Type: multipart/form-data
```

> request

```
String name
byte[] planImage required // 平面图
float planSize required // 平面图大小
String planUnit // 平面图单位 default 'px'
float[] actualSize required // 实际大小
String actualUnit // 实际单位 default 'm'
float[] originInPlan //原点在平面图的位置
```

> response

```json
// success
{
    code: 0,
    data: {
        id: 'ddss', // map的Id
        name: '',
        planPath: 'xxx',
        planSize: [0, 0, 0],
        planUint: 'px',
        actualSize: [0, 0, 0],
        actualUnit: 'm',
        orginInPlan: [0, 0, 0]
        modelPath: ''
    }
}

// fail
{
    code: 1
    msg: '新建地图失败'
}
```

##### 2. 添加点到地图

```
/post POST
content-type: application/json
```

> request:

```json
{
	mapId: String, // required
	planCoordinate: {
		x: int, //
		y: int,
		z: int
	}, // 在平面图的位置 default {x: 0, y: 0, z: 0}
	actualCoordinate: {
		x: int,
		y: int,
		z: int
	} // 实际的位置 default = planCoordinate
}
```

> response

```json
// success
{
    code: 1,
    data: {
        id: 'xx',
        mapId: 'xx',
       	planCoordinate: {
            x: 0, //
            y: 0,
            z: 0
        },
        actualCoordinate: {
            x: 0,
            y: 0,
            z: 0
        }, // 实际
    	adjacence: ['id1', 'id2'] // 相连的点
    }
}
// fail
{
    code: 0,
    msg: '添加坐标失败'
}
```

##### 3. 添加路径到map

```
/edge POST
Content-Type: application/x-www-form-urlencoded
```

> request

```
String pointAId,// required
String pointBId,// required
String mapId // 
```

> response

```json
// success
{
   	code: 0,
   	data: {
   		id: 'xxx',
        pointA: {
            id: 'xx',
            planCoordinate: {
                x: 0, //
                y: 0,
                z: 0
            },
            actualCoordinate: {
                x: 0,
                y: 0,
                z: 0
            }, // 实际
        },
    	pointB: //同pointA
	}
}

// fail
{
    code: 0,
    msg: '失败'
}
```

#### 获取地图信息

```
/map
```

> request

```
String mapId  // required
int includePoint // 是否包含点 1表示包含
int includeEdge // 是否包含边 1表示包含
```

> response

```json
{
    code: 0,
    data: [{
        map: {
            id: 'ddss', // map的Id
            name: '',
            planPath: 'xxx',
            planSize: [0, 0, 0],
            planUint: 'px',
            actualSize: [0, 0, 0],
            actualUnit: 'm',
            orginInPlan: [0, 0, 0]
            modelPath: ''
        },
        points: [ // includePoint = 1时
            {
                id: 'xx',
                mapId: 'xx',
                planCoordinate: {
                    x: 0, //
                    y: 0,
                    z: 0
                },
                actualCoordinate: {
                    x: 0,
                    y: 0,
                    z: 0
                }, // 实际
                adjacence: ['id1', 'id2'] // 相连的点
            }
        ],
        edge: [{ // includeEdge 为1时
            id: 'xxx',
            pointA: {
                id: 'xx',
                planCoordinate: {
                    x: 0, //
                    y: 0,
                    z: 0
                },
                actualCoordinate: {
                    x: 0,
                    y: 0,
                    z: 0
                }, // 实际
            },
            pointB: //同pointA
        }]
    }]
}
```

#### 获取地图上的点

```
/point GET
```

> request

```
String mapId
```

> response

```// success
{
    code: 0,
    data: [{
        id: 'xx',
        mapId: 'xx',
       	planCoordinate: {
            x: 0, //
            y: 0,
            z: 0
        },
        actualCoordinate: {
            x: 0,
            y: 0,
            z: 0
        }, // 实际
    	adjacence: ['id1', 'id2'] // 相连的点
    }]
}
// fail
{
    code: 0,
    msg: 'xxx'
}
```



#### 根据起点搜索

```
/loc/search GET
```

> request

```
String name
```

> response

```
// success
{
	code: 0;
	data: [{
		loc: {
			name: 'xxx',
			planCoordinate: {
                x: 0, //
                y: 0,
                z: 0
            },
            actualCoordinate: {
                x: 0,
                y: 0,
                z: 0
            }, // 实
		},
		map: {
			id: 'ddss', // map的Id
            name: '',
            planPath: 'xxx',
            planSize: [0, 0, 0],
            planUint: 'px',
            actualSize: [0, 0, 0],
            actualUnit: 'm',
            orginInPlan: [0, 0, 0]
            modelPath: ''
		}
	}]
}
```

#### 根据终点和mapId搜索

```
/loc/search GET
```

> request

```
String name,
String mapId
```

> response

```
// success
{
	code: 0;
	data: [{
		loc: {
			id: 'xxx'
			name: 'xxx',
			planCoordinate: {
                x: 0, //
                y: 0,
                z: 0
            },
            actualCoordinate: {
                x: 0,
                y: 0,
                z: 0
            }, // 实
		},
		map: {
			id: 'ddss', // map的Id
            name: '',
            planPath: 'xxx',
            planSize: [0, 0, 0],
            planUint: 'px',
            actualSize: [0, 0, 0],
            actualUnit: 'm',
            orginInPlan: [0, 0, 0]
            modelPath: ''
		}
	}]
}
```

#### 根据起点和终点获取路径

```
/map/navi POST
Content-Type: application/json
```

> request

```json
{
	"mapId": "5e68ca6e148277137d1c62b1",
	"src": {
        "actualCoordinate": {
            "x": 0,
            "y": 0,
            "z": 0
        }
	},
	"dst": {
        "actualCoordinate": {
            "x": 70,
            "y": -50,
            "z": 0
        }
	}
}
```

> response

```json
{
    "code": 0,
    "data": {
        "pathId": "eaa90fcc651d11eaa492001e64cce6eb",
        "path": [
            {
                "id": "5e6b0be0e4f26f3c4a8dadc0",
                "mapId": "5e68ca6e148277137d1c62b1",
                "planCoordinate": {
                    "x": 0,
                    "y": 0,
                    "z": 0
                },
                "actualCoordinate": {
                    "x": 0,
                    "y": 0,
                    "z": 0
                },
                "adjacence": [
                    "5e6b0c06e4f26f3c4a8dadc2",
                    "5e6b0c1ae4f26f3c4a8dadc6"
                ]
            },
            {
                "id": "5e6b0c1ae4f26f3c4a8dadc6",
                "mapId": "5e68ca6e148277137d1c62b1",
                "planCoordinate": {
                    "x": 0,
                    "y": -50,
                    "z": 0
                },
                "actualCoordinate": {
                    "x": 0,
                    "y": -50,
                    "z": 0
                },
                "adjacence": [
                    "5e6b0c0fe4f26f3c4a8dadc4",
                    "5e6b0be0e4f26f3c4a8dadc0"
                ]
            },
            {
                "id": "5e6b0c0fe4f26f3c4a8dadc4",
                "mapId": "5e68ca6e148277137d1c62b1",
                "planCoordinate": {
                    "x": 70,
                    "y": -50,
                    "z": 0
                },
                "actualCoordinate": {
                    "x": 70,
                    "y": -50,
                    "z": 0
                },
                "adjacence": [
                    "5e6b0c06e4f26f3c4a8dadc2",
                    "5e6b0c1ae4f26f3c4a8dadc6"
                ]
            }
        ]
    }
}
```

#### 添加关键点

```
/loc POST
content-type: application/json
```

> request

```json
{
	mapId: "xxxxx",
    name: "超算五楼",
    planCoordinate: {
        x: 1,
        y: 1,
        z: 1
    },
    actualCoordinate: {
        x: 1,
        y: 1,
        z: 1
    }
}
```

> response

```json
{
    "code": 0,
    "data": {
        "id": "5e68edb2170102f1cffc97a8",
        "mapId": "xxxxx",
        "name": "超算五楼",
        "planCoordinate": {
            "x": 1,
            "y": 1,
            "z": 1
        },
        "actualCoordinate": {
            "x": 1,
            "y": 1,
            "z": 1
        }
    }
}
```





#### 训练deep-navi模型

```
/map/train POST
content-type multipart/form-data

```

> request

```
// 先搞清楚怎么训练
```

> response

```json
{
    code: 0,
    msg: '添加训练数据成功'
}
```

