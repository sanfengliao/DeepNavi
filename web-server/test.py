import dao.point

pointDao = dao.point.PointDao()

result = pointDao.findPointsIn(['5e64de9265bf8bca60db5865', '5e64deb3bd4b95cd35f1430f'], '5e64cfce8aeb3647322e0880')
for item in result:
    print(item.toJsonMap())