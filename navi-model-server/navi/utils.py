"""
utils
"""
import shutil
import torch
import os
import numpy as np
from datetime import datetime
import time
import pandas as pd
from losses import *
from datasets import *
import bisect
from tqdm import tqdm
import re

def save_checkpoint(state, is_best, filename, dir):
    new_filename = os.path.join(dir, filename)
    torch.save(state, new_filename + '.pth.tar.temp')
    if is_best:
        shutil.copyfile(new_filename + '.pth.tar.temp', new_filename + '.pth.tar')

def load_checkpoint(model, model_dir):
    model_tags = os.listdir(model_dir)
    model_tags.sort()
    last_model = model_tags[-1]

def ouput_sample_data(timestamp, features, targets, trans, rotation):
    root = "fuse_output"
    print("output timestamp", timestamp)
    t = str(timestamp)
    this_dir = os.path.join(root, t[0] + t[2:])
    if not os.path.exists(this_dir):
        os.mkdir(this_dir)

    if "img" in features:
        np.savetxt(os.path.join(this_dir, "img_feature.csv"), features["img"].reshape((-1, 1)), delimiter=',')
    if "mag" in features:
        np.savetxt(os.path.join(this_dir, "mag_feature.csv"), features["mag"].reshape((-1, 1)), delimiter=',')
    np.savetxt(os.path.join(this_dir, "fusion_feature.csv"), features["fusion"].reshape((-1, 1)), delimiter=',')
    np.savetxt(os.path.join(this_dir, "target.csv"), targets, delimiter=',')
    np.savetxt(os.path.join(this_dir, "trans.csv"), trans, delimiter=',')
    if rotation is not None:
        np.savetxt(os.path.join(this_dir, "rotation.csv"), rotation, delimiter=',')

def get_time(time_str):
    # eg.20180710_152235_462 to 1531207355.462
    # timestamp = time.time() : 1551861367.821322
    # datetime.fromtimestamp(timestamp): 2019-03-06 16:36:07.821322
    # datetime.strptime("2019-03-06 16:36:07.821322", "%Y-%m-%d %H:%M:%S.%f")

    time_raw = time_str.split('_')
    time_raw = datetime(
        int(time_raw[0][ : 4]), # year
        int(time_raw[0][4: 6]), # month
        int(time_raw[0][6: 8]), # day
        int(time_raw[1][ : 2]), # hour
        int(time_raw[1][2: 4]), # minite
        int(time_raw[1][4: 6]), # second
        1000 * int(time_raw[2]), # nanosecond
    )
    original_string = str(time_raw.timestamp())
    return original_string[:10] + original_string[11:]

def change_matecsv_to_sensorcsv(file):
    data = np.loadtxt(file, delimiter=",", dtype=str)
    for i in range(1, data.shape[0]):
        data[i, -1] = get_time(data[i, -1])

    dir, _ = os.path.split(file)
    header = ''.join(c + "," for c in data[0])[:-1]
    print(header)
    np.savetxt(os.path.join(dir, "sensor.csv"), data, delimiter=",", fmt="%s")

    # usage: change_matecsv_to_sensorcsv("/home/huangjianjun/dataset/chaosuan_new/gogo/mate.csv")

def split_sensorcsv(file):
    data = np.loadtxt(file, delimiter=",", dtype=str)
    print("data shape", data.shape)

    # split way
    split_ratio = 0.6
    seperate_point = int(data.shape[0] * 0.6)

    # split
    train_data = data[:seperate_point]
    print("train data shape", train_data.shape)
    val_data = data[seperate_point:]
    print("val data shape", val_data.shape)

    # split way paper
    seperate_point = 3000
    end = 5600
    train_data = data[:seperate_point]
    print("train data shape", train_data.shape)
    val_data = data[seperate_point:end]
    print("val data shape", val_data.shape)

    # save
    dir, _ = os.path.split(file)
    np.savetxt(os.path.join(dir, "train_loc_paper.csv"), train_data, delimiter=',', fmt="%s")
    np.savetxt(os.path.join(dir, "val_loc_paper.csv"), val_data, delimiter=',', fmt="%s")


class AverageMeter(object):
    """Computes and stores the average and current value"""
    def __init__(self):
        self.reset()

    def reset(self):
        self.val = 0
        self.avg = 0
        self.sum = 0
        self.count = 0

    def update(self, val, n=1):
        self.val = val
        self.sum += val * n
        self.count += n
        self.avg = self.sum / self.count

class AverageMeterRecording(AverageMeter):
    """Computes and stores the average and current value, and recording each value"""
    def __init__(self):
        super(AverageMeterRecording, self).__init__()
        self.values = []

    def update(self, val, n=1):
        super(AverageMeterRecording, self).update(val, n)
        self.values.append(val)

    def to_file(self, file_name):
        values_array = np.transpose(np.array(self.values))
        np.savetxt(file_name, values_array, delimiter=',')

class Recorder(object):
    def __init__(self):
        self.data = list()
        self.timestamps = list()

    def add(self, value_, batched=False):
        # list, Tensor to numpy
        value = value_
        if isinstance(value, torch.Tensor):
            value = value.cpu().numpy()
        if isinstance(value, list):
            value = np.array(value)

        if not batched:
            self.data.append(value)
            self.timestamps.append(time.time())
        else:
            ts = time.time()
            for i in range(value.shape[0]):
                self.data.append(value[i])
                self.timestamps.append(ts)

    def to_file(self, filename, with_timestamp=False):
        name, ext = os.path.splitext(filename)
        assert ext == ".csv"

        if with_timestamp:
            csv_data = self.data
        else:
            csv_data = np.array(self.data)

        np.savetxt(filename, csv_data, delimiter=',')

def bootstrap(indexes):
    idx = np.random.choice(indexes, indexes.shape, replace=True)
    return idx


def combine_base_model(model_tag_list):
    bm_results = []
    bm_results_rot = []
    for base_model in model_tag_list:
        res = np.loadtxt(os.path.join("/home/huangjianjun/workdir/navi/output/", base_model, "trans_estimation.csv"), delimiter=',')
        bm_results.append(res)
        res_1 = np.loadtxt(os.path.join("/home/huangjianjun/workdir/navi/output/", base_model, "rot_estimation.csv"), delimiter=',')
        bm_results_rot.append(res_1)

    bm_results = np.array(bm_results)
    combination = np.mean(bm_results, axis=0)
    bm_results_rot = np.array(bm_results_rot)
    combination_rot = np.mean(bm_results_rot, axis=0)

    output_dir = "/home/huangjianjun/workdir/navi/output/combination"
    if not os.path.exists(output_dir):
        os.mkdir(output_dir)

    np.savetxt(os.path.join(output_dir, "trans_estimation.csv"), combination, delimiter=',')
    np.savetxt(os.path.join(output_dir, "rot_estimation.csv"), combination_rot, delimiter=',')

    gt = np.loadtxt("/home/huangjianjun/dataset/chaosuan_new/zhixian/val_loc.csv", dtype=np.str, delimiter=',')
    gt_loc = gt[:, 1:4].astype(np.float)
    gt_rot = gt[:, 4:8].astype(np.float)
    loc_error = np.linalg.norm(combination - gt_loc, axis=1)
    rot_error = np.zeros(loc_error.shape)
    for i in range(rot_error.shape[0]):
        rot_error[i] = rotation_error(torch.from_numpy(np.expand_dims(combination_rot[i], axis=0)),
                                   torch.from_numpy(np.expand_dims(gt_rot[i], axis=0)))
    np.savetxt(os.path.join(output_dir, "trans_error.csv"), loc_error, delimiter=',')
    np.savetxt(os.path.join(output_dir, "rotation_error.csv"), rot_error, delimiter=',')

def delete_empty_filed():
    # delete the last empty field
    file = "/home/huangjianjun/dataset/chaosuan_new/gogo/val_loc.csv"
    gt = np.loadtxt(file, dtype=np.str, delimiter=',')
    gt = gt[:, :8]
    np.savetxt(file, gt, fmt="%s", delimiter=',')


def find_closet_item(timestamp, timestamp_list):
    indx_right = bisect.bisect_left(timestamp_list, timestamp)
    indx_left = indx_right - 1 if indx_right > 0 else 0
    indx_right = indx_right - 1 if indx_right == len(timestamp_list) else indx_right
    time_left = timestamp_list[indx_left]
    time_right = timestamp_list[indx_right]
    err_left = timestamp - time_left
    err_right = time_right - timestamp

    if err_left < err_right:
        return indx_left, time_left, err_left
    else:
        return indx_right, time_right, err_right


def inclusive_range(start, stop, step):
    return range(start, (stop + 1) if step >= 0 else (stop - 1), step)

def get_value(key_value_arr, keys, max_value=-120):
    res = []
    for k in keys:
        t = key_value_arr[np.where(key_value_arr[:, 0] == k)]
        if t.size != 0:
            res.append(t[0, 1])
        else:
            res.append(max_value)  # if there is no that mac, it means too far to catch the data, so use the min rssi value
    return np.array(res)

def create_wifi_data():
    data_root = '/home/huangjianjun/dataset/chaosuan_new/gogo/'
    wifi_max_data = -120  # zhixian
    wifi_max_data = -80  #gogo
    # loc_wifi_idx is coorespoing to labels_wifi
    # wifi_data_dir = 'wifi_data'  # zhixian
    wifi_data_dir = 'wifi/data_gogo/'  # gogo
    # zhixian
    # wifi_labels_file_name = 'labels_wifi.csv'
    # train_loc_idx_file_name = 'train_loc_wifi_idx.csv'
    # val_loc_idx_file_name = 'val_loc_wifi_idx.csv'
    # gogo
    index = 10
    wifi_labels_file_name = 'gogo_wifi_data/label_wifi_' + str(index) + '.csv'
    train_loc_idx_file_name = 'gogo_wifi_data/train_loc_wifi_idx_' + str(index) + '.csv'
    val_loc_idx_file_name = 'gogo_wifi_data/val_loc_wifi_idx_' + str(index) + '.csv'
    all_loc_file_name = 'vins_result_loop.csv'

    # create loc data
    all_loc = np.loadtxt(os.path.join(data_root, all_loc_file_name), delimiter=',', dtype=np.str)
    train_idx = np.loadtxt(os.path.join(data_root, train_loc_idx_file_name), delimiter=',', dtype=np.int)
    train_loc = all_loc[train_idx - 1]  # Matlab idx begin from 1, python begin from 0
    np.savetxt(os.path.join(data_root, "gogo_wifi_data/train_loc_wifi_" + str(index) + ".csv"), train_loc[:, :-1], delimiter=',',
               fmt='%s')  # delete last empty column
    val_idx = np.loadtxt(os.path.join(data_root, val_loc_idx_file_name), delimiter=',', dtype=np.int)
    val_loc = all_loc[val_idx - 1]  # Matlab idx begin from 1, python begin from 0
    np.savetxt(os.path.join(data_root, "gogo_wifi_data/val_loc_wifi_" + str(index) + ".csv"), val_loc[:, :-1], delimiter=',',
               fmt='%s')  # delete last empty column

    wifi_labels = np.loadtxt(os.path.join(data_root, wifi_labels_file_name), dtype=np.str)
    vfunc = np.vectorize(lambda x: x.replace(',', '.'))  # add float point
    wifi_labels = vfunc(wifi_labels)
    # macs = np.loadtxt(os.path.join(data_root, 'mac.txt'), dtype=np.str)  # zhixian
    macs = np.loadtxt('/home/huangjianjun/dataset/chaosuan_new/gogo/wifi/data_gogo/common_wifl_all/mac50.txt', dtype=np.str)
    train_data = []
    val_data = []
    for l in tqdm(wifi_labels):
        even_wifi_values = []
        for i in range(0, 5, 2):
            t = np.loadtxt(os.path.join(data_root, wifi_data_dir, l, str(i) + '.txt'), dtype=np.str)
            values = get_value(t, macs, max_value=wifi_max_data).astype(np.float)
            # assert values.size == 10  # zhixian
            # assert values.size == 10
            even_wifi_values.append(values)

        even_wifi_values = np.array(even_wifi_values)
        even_wifi_values = np.mean(even_wifi_values, axis=0)
        train_data.append(even_wifi_values)

        odd_wifi_values = []
        for i in range(1, 5, 2):
            t = np.loadtxt(os.path.join(data_root, wifi_data_dir, l, str(i) + '.txt'), dtype=np.str)
            values = get_value(t, macs, max_value=wifi_max_data).astype(np.float)
            # assert values.size == 10
            odd_wifi_values.append(values)

        odd_wifi_values = np.array(odd_wifi_values)
        odd_wifi_values = np.mean(odd_wifi_values, axis=0)
        val_data.append(odd_wifi_values)

    # add timestamp same as loc
    # val_loc[:, 0]  (164, 8) to (164, )  concatenate should make shape (164, 1)
    # so use np.expand_dims or val_loc[:, [0]] val_loc[:, 0:1]
    train_data = np.array(train_data)
    train_data = np.concatenate((train_loc[:, [0]], train_data), axis=1)
    val_data = np.array(val_data)
    val_data = np.concatenate((np.expand_dims(val_loc[:, 0], axis=1), val_data), axis=1)

    np.savetxt(os.path.join(data_root, "gogo_wifi_data/train_wifi_50mac_" + str(index) + ".csv"), train_data, delimiter=',', fmt="%s")
    np.savetxt(os.path.join(data_root, "gogo_wifi_data/val_wifi_50mac_" + str(index) + ".csv"), val_data, delimiter=',', fmt="%s")


def construct_val_wifi_dataset_from_train():
    # finding the nearest loc of train data

    dataset1 = LocDataset(dataroot="/home/huangjianjun/dataset/chaosuan_new/zhixian/",
                         filename="train_loc_wifi.csv")
    all_data_expcept = LocDataset(dataroot="/home/huangjianjun/dataset/chaosuan_new/zhixian/",
                         filename="vins_result_loop.csv")

    start_point = 2690
    val_dataset_idx = []
    for i in tqdm(range(133, len(dataset1))):
        train_loc = dataset1[i]
        train_loc = torch.unsqueeze(train_loc, dim=0)

        pre_error = 100
        for j in range(start_point, len(all_data_expcept), 3):
            print("now search point", j)
            search_loc = all_data_expcept[j]
            search_loc = torch.unsqueeze(search_loc, dim=0)
            error = pose_loss(train_loc, search_loc).numpy()
            print("error", error)
            error_change = error - pre_error
            print("error change", error_change)
            pre_error = error
            if error_change > 0:
                print("new val point", j - 1)
                val_dataset_idx.append(j - 1)
                print("new val set idx", val_dataset_idx)
                start_point = j
                break

def construct_wifi_dataset_using_interplolation(index_, prefix_):
    index = 4
    index = index_
    prefix = 'train'
    # prefix = 'val'
    prefix = prefix_
    dataset_original = LocDataset(dataroot="/home/huangjianjun/dataset/chaosuan_new/gogo/gogo_wifi_data/",
                         filename=prefix + "_loc_wifi_" + str(index) + ".csv")
    dataset_all = LocDataset(dataroot="/home/huangjianjun/dataset/chaosuan_new/gogo/",
                                  filename="vins_result_loop.csv")
    new_dataset_idx = []
    insert_num = 2
    for i in range(len(dataset_original) - 1):
        begin = dataset_original.indexes[i]
        end = dataset_original.indexes[i + 1]

        # print("begin time", begin)
        b_idx, b_time, b_err = find_closet_item(begin, dataset_all.indexes)
        e_idx, e_time, e_err = find_closet_item(end, dataset_all.indexes)
        # print("b", b_idx, b_time, b_err)
        # print("e", e_idx, e_time, e_err)

        # print(np.linspace(b_idx, e_idx, insert_num + 2))
        # print(np.round(np.linspace(b_idx, e_idx, insert_num + 2)))
        idx = np.round(np.linspace(b_idx, e_idx, insert_num + 2)).astype(np.int)
        if i == len(dataset_original) - 2:
            new_dataset_idx.extend(idx.tolist())
        else:
            except_last_idx = idx[:-1]
            # print(except_last_idx)
            new_dataset_idx.extend(except_last_idx.tolist())

    # print(new_dataset_idx)
    raw_data =np.loadtxt("/home/huangjianjun/dataset/chaosuan_new/gogo/vins_result_loop.csv", delimiter=',',
                            dtype=np.str)
    np.savetxt("/home/huangjianjun/dataset/chaosuan_new/gogo/gogo_wifi_data/" + prefix + "_loc_wifi_inter_" + str(index) + ".csv",
               raw_data[new_dataset_idx, :8], fmt="%s", delimiter=',')

    ###################################
    # interplole wifi data
    ####################################
    raw_wifi_data = np.loadtxt("/home/huangjianjun/dataset/chaosuan_new/gogo/gogo_wifi_data/" + prefix + "_wifi_" + str(index) + ".csv",
                               delimiter=',', dtype=np.str)
    raw_wifi_data = np.loadtxt(
        "/home/huangjianjun/dataset/chaosuan_new/gogo/gogo_wifi_data/" + prefix + "_wifi_50mac_" + str(index) + ".csv",
        delimiter=',', dtype=np.str)
    wifi_data = raw_wifi_data[:, 1:].astype(np.float)
    new_wifi_data = np.zeros((len(new_dataset_idx), wifi_data.shape[1]), dtype=np.float)

    ori_len = len(dataset_original)
    for i in range(ori_len + (ori_len - 1) * 2):
        idx = i / 3
        # print(idx)
        if i % 3 == 0:
            new_wifi_data[i] = wifi_data[int(idx)]
        else:
            ceil = np.ceil(i / 3).astype(np.int)
            floor = np.floor(i / 3).astype(np.int)
            # print(ceil)
            # print(floor)
            delta = wifi_data[ceil] - wifi_data[floor]
            new_data = wifi_data[floor] + delta * (idx - floor)
            # print(new_data)
            new_wifi_data[i] = new_data

    new_wifi_data = np.concatenate((raw_data[new_dataset_idx, 0:1], new_wifi_data.astype(np.str)), axis=1)
    np.savetxt("/home/huangjianjun/dataset/chaosuan_new/gogo/gogo_wifi_data/" + prefix + "_wifi_inter_50mac_" + str(index) + ".csv",
               new_wifi_data, fmt="%s", delimiter=',')

def gogo_wifi_func():
    data_root = "/home/huangjianjun/dataset/chaosuan_new/gogo/wifi/data_gogo/"
    dirs = [d for d in os.listdir(data_root) if d[0].isdigit()]
    file_name = "sensor.txt"

    # # split one file to more files with blank lines
    # for dir in dirs:
    #     partion = []
    #     with open(os.path.join(data_root, dir, file_name), "r", encoding="utf-8") as f:
    #         lines = f.readlines()
    #
    #         temp_str_list = []
    #         for l in lines:
    #             if l == '\n' and len(temp_str_list) != 0:
    #                 partion.append(temp_str_list)
    #                 temp_str_list = []
    #             elif l != '\n':
    #                 l.strip()
    #                 temp_str_list.append(l)
    #
    #     for i, p in enumerate(partion):
    #         with open(os.path.join(data_root, dir, str(i) + "_original.txt"), 'w', encoding='utf-8') as f:
    #             f.writelines(p)
    #
    #         # remove ssid, make the data the same with zhixian
    #         res = []
    #         for line in p:
    #             l = []
    #             elements = line.split()
    #             mac = elements[0][6:]
    #             strength = elements[-1][6:]
    #             # ssid_begin_idx = line.find('ssid', 3) + 5  #第二个ssid的冒号竟然是中文字体
    #             # ssid_end_idx = line.find('level:')
    #             # ssid = line[ssid_begin_idx:ssid_end_idx]
    #             l.append(mac)
    #             l.append(strength)
    #             # l.append(ssid)
    #             res.append(l)
    #         # with open(os.path.join(data_root, dir, str(i) + "_ssid.txt"), 'w+', encoding='utf-8') as f:
    #         #     np.savetxt(f, np.array(res), fmt='%s', delimiter=',')  # not use space for delimiter, cause some ssids contain space
    #         np.savetxt(os.path.join(data_root, dir, str(i) + ".txt"), np.array(res), fmt='%s')

    # find the common wifi
    point_list = ["0." + str(i) for i in range(1, 16)]
    point_list = point_list + ["1." + str(i) for i in range(3, 26)]
    point_list = None
    find_common_wifi(data_root, point_list=point_list)

def find_common_wifi(data_root, point_list=None):
    if point_list is None:
        point_list = [d for d in os.listdir(data_root) if d[0].isdigit()]

    # find common elements for each point, cause for each point we will sample many data, if needed
    for p in point_list:
        files = os.listdir(os.path.join(data_root, p))
        wifi_files = [f for f in files if re.match(r"\d.txt", f)]
        wifi_macs = []
        mac_to_ssid = dict()
        for wf in wifi_files:
            # if read unicode file
            # with open(os.path.join(data_root, p, wf), "r", encoding="utf-8") as f:
            #     raw_data = np.loadtxt(f, dtype=np.str, delimiter=',')
            raw_data = np.loadtxt(os.path.join(data_root, p, wf), dtype=np.str)
            wifi_mac = raw_data[:, 0]
            # for i, m in enumerate(wifi_mac):
            #     if m not in mac_to_ssid:
            #         mac_to_ssid[m] = raw_data[i, 2]
            wifi_macs.append(wifi_mac.tolist())

        common_wifi_mac = common_elements_in_list_of_list(wifi_macs)
        # common_wifi_ssid = []
        # for m in common_wifi_mac:
        #     common_wifi_ssid.append(mac_to_ssid[m])
        # data = np.concatenate((np.transpose([common_wifi_mac]), np.transpose([common_wifi_ssid])), axis=1)
        np.savetxt(os.path.join(data_root, p, "common_wifi_mac_ssid.txt"), np.array(common_wifi_mac), fmt="%s")
        # with open(os.path.join(data_root, p, "common_wifi_mac_ssid.txt"), "w+", encoding="utf-8") as f:
        #     np.savetxt(f, data, fmt="%s", delimiter=',')

    # find common elements for point_list

    wifi_macs = []
    mac_to_ssid = dict()
    for p in point_list:
        wifi_mac = np.loadtxt(os.path.join(data_root, p, "common_wifi_mac.txt"), dtype=np.str)
        # with open(os.path.join(data_root, p, "common_wifi_mac_ssid.txt"), "r", encoding="utf-8") as f:
        #     wifi_mac = np.loadtxt(f, dtype=np.str, delimiter=',')
        wifi_macs.append(wifi_mac.tolist())
        # for i, m in enumerate(wifi_mac[:, 0]):
        #     if m not in mac_to_ssid:
        #         mac_to_ssid[m] = wifi_mac[i, 1]
        # wifi_macs.append(wifi_mac[:, 0].tolist())

    common_wifi_mac = common_elements_in_list_of_list(wifi_macs)
    count_wifi_mac = count_elements_list_of_list(wifi_macs)
    np.savetxt(os.path.join(data_root, "common_wifi_mac.txt"), np.array(common_wifi_mac), fmt="%s")
    np.savetxt(os.path.join(data_root, "count_wifi_mac.txt"), np.array(count_wifi_mac), fmt="%s")

    # data = [(e[0], e[1], mac_to_ssid[e[0]]) for e in count_wifi_mac]
    # with open(os.path.join(data_root, "count_wifi_mac_ssid.txt"), "w+", encoding="utf-8") as f:
    #     np.savetxt(f, np.array(data), fmt="%s", delimiter=',')

def common_elements_in_list_of_list(lst_of_lst):
    res = list(set.intersection(*map(set, lst_of_lst)))
    res.sort()
    return res

def count_elements_list_of_list(lst_of_lst):
    appearance_count = dict()
    for lst in lst_of_lst:
        for e in lst:
            appearance_count[e] = appearance_count.get(e, 0) + 1
    # sort by value
    sorted_x = sorted(appearance_count.items(), key=lambda kv: kv[1], reverse=True)
    return sorted_x

def create_label_wifi_file():
    output_file_name = "/home/huangjianjun/dataset/chaosuan_new/gogo/labels_wifi.csv"
    labels = ["0." + str(i) for i in range(1, 16)]
    np.savetxt(output_file_name, np.array(labels), fmt="%s")


def cat_gogo_wifi_data():
    data_root = "/home/huangjianjun/dataset/chaosuan_new/gogo/gogo_wifi_data/"
    file_name_prefix = "val_wifi_50mac"
    data = []
    for i in range(1, 1+10):
        now_data = np.loadtxt(os.path.join(data_root, file_name_prefix + "_" + str(i) + ".csv"), delimiter=",", dtype=np.str)
        data.append(now_data)
    data = np.concatenate(data, axis=0)
    np.savetxt(os.path.join("/home/huangjianjun/dataset/chaosuan_new/gogo", file_name_prefix + ".csv"), data, delimiter=",", fmt="%s")

def func():
    # get part of the loss
    loss_path = "/home/huangjianjun/workdir/navi/model_weights/img_wifi_new_inter_2019_04_21_17_47_55/loss.csv"
    output_file = "/home/huangjianjun/workdir/navi/temp_loss.csv"

    data = np.loadtxt(loss_path, delimiter=',')
    output_data = data[:, [0, 3, 4]]
    np.savetxt(output_file, output_data, delimiter=',', header="epoch,train_localization_err,train_orientation_err")
    print(data[0])

if __name__ == "__main__":
    # split_sensorcsv("/home/huangjianjun/dataset/chaosuan_new/gogo/vins_result_loop.csv")
    # create_wifi_data()
    # construct_val_wifi_dataset_from_train()
    # for i in range(1, 11):
    #     for prefix in ["train", "val"]:
    #         construct_wifi_dataset_using_interplolation(i, prefix)
    # gogo_wifi_func()
    # create_label_wifi_file()
    cat_gogo_wifi_data()
    # func()
