import csv
f = open('./loc.csv', 'r')
data = csv.DictReader(f, ['x', 'y', 'z'])
print(dict(data))