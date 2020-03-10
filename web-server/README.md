#### 创建一个地图

创建一个地图分三个步骤

##### 1. 新建一个地图

```
/map/new POST
Content-Type: multipart/form-data
```

> request

```
String name
byte[] planImage required // 平面图
int[] planSize required // 平面图大小
String plantUnix // 平面图单位 default 'px'
int[] actualSize required // 实际大小
String actualUnit // 实际单位 default 'm'
int[] originInPlan //原点在平面图的位置
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
/map/addPoint POST
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
/map/addPath POST
Content-Type: 只要不是form-data
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

#### 获取地图所有的点

```
/map/points GET
```

> request

```
String mapId
```

> response

```json
// success
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
/map/search GET
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
/map/search GET
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

```
{
	start: {
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
	end: {
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
	}
}
```

> response

```
// success
{
	code: 0,
	// 第一个元素和最后一个元素是起点和终点
	data: [{
		{
			pointId: 'xxxx',
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
	}]
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
