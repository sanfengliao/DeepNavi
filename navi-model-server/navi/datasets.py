"""
datasets
"""
import os
import bisect
import functools
import numpy as np
import torch
from torch.utils import data
from .utils import *
from .config import *
import torchvision.models as models
from PIL import Image

def default_loader(path):
    return Image.open(path).convert('RGB')

class ImageSeriesFolder(data.Dataset):
    """
    dataroot: root directory of images
    images: its file name is timestamp
    """
    def __init__(self, data_dir, loader=None, transform=None):
        """
        init
        """
        print('constructing image dataset')
        # load timestamp(image file name) andcoresponding image filename, then sort them
        self.data_dir = data_dir
        self.loader = default_loader if loader is None else loader 
        self.transform = transform
        self.indexes = [os.path.splitext(x)[0] for x in os.listdir(data_dir)]
        self.filenames = np.array(self.indexes)
        self.indexes = np.array(list(map(lambda x: float(x[:1] + '.' + x[1:]), self.indexes)))
        self.filenames = self.filenames[np.argsort(self.indexes)]
        self.indexes.sort()
        print("There are {} data".format(self.__len__()))

    def __len__(self):
        """
        len
        """
        return len(self.indexes)

    def __getitem__(self, idx):
        """
        get item
        return a PIL Image
        """
        img = self.loader(os.path.join(
            self.data_dir,
            str(self.filenames[idx]) + '.png'))
        if self.transform is not None:
            img = self.transform(img)

        # if bached, size: [B, C, H, W]
        return img

class MagDataset(data.Dataset):
    """
    mag dataset
    reading from a csv file
    """
    def __init__(self, file, scaler=None):
        """
        init
        """
        print('constructing magnetic dataset')
        self.scaler = scaler
        self.rawdata = np.loadtxt(
            file,
            dtype=np.str,
            delimiter=",",
            skiprows=1)

        # convert timestamp to float
        self.indexes = self.rawdata[:, 0]
        self.data = self.rawdata[:, 1:].astype(np.float32)
        vfunc = np.vectorize(lambda x: x[:1] + '.' + x[1:])
        self.indexes = vfunc(self.indexes).astype(np.float64)

        #sort by timestamp
        self.data = self.data[np.argsort(self.indexes)]

        # fit training data and normalize them
        if self.scaler is not None:
            self.data = self.scaler.fit_transform(self.data)

        # from numpy array to pytorch tensor
        self.data = torch.from_numpy(self.data).float()

        # also sort the timestamp
        self.indexes.sort()
        print("There are {} data".format(self.__len__()))
        assert self.indexes.shape[0] == self.data.size(0)

    def __len__(self):
        """
        len
        """
        return len(self.indexes)

    def __getitem__(self, idx):
        """
        get item
        return a numpy float array with shape (3)
        """
        return self.data[idx]


class WiFiDataset(data.Dataset):
    """
    wifi dataset
    reading from a csv file
    """
    def __init__(self, dataroot, filename, mode="Train", scaler=None):
        """
        init
        """
        print('constructing wifi dataset')
        self.dataroot = dataroot
        assert mode in ["Train", "Val"]
        self.mode = mode
        self.filename = filename
        self.scaler = scaler
        self.rawdata = np.loadtxt(
            os.path.join(self.dataroot, self.filename),
            dtype=np.str,
            delimiter=","
        )

        # convert timestamp to float
        self.indexes = self.rawdata[:, 0]  # timestamp
        self.data = self.rawdata[:, 1:].astype(np.float32)  # Wi-Fi data 10 demension
        vfunc = np.vectorize(lambda x: x[:1] + '.' + x[1:])  # add float point
        self.indexes = vfunc(self.indexes).astype(np.float64)

        #sort by timestamp
        self.indexes.sort()
        self.data = self.data[np.argsort(self.indexes)]

        # fit training data and normalize them
        if self.scaler is not None and self.mode == "Train":
            # the mean data is a vector, shape is same as data[0], each channel a mean, not all data a mean
            self.data = self.scaler.fit_transform(self.data)
            # if needed, output mean and var for testing
            # print("mean of data: ", self.scaler.mean_)
            # print("variance of data: ", self.scaler.var_)
        elif self.mode == "Val":
            self.data = self.scaler.transform(self.data)

        # from numpy array to pytorch tensor
        self.data = torch.from_numpy(self.data).float()

        print("There are {} data".format(self.__len__()))

    def __len__(self):
        return len(self.data)

    def __getitem__(self, idx):
        return self.data[idx]

def LocaDatasetSampler(start, end, num):
    if start is None and end is None and num is not None:
        return lambda x: x[::len(x) // num]
    elif start is not None and end is None and num is None:
        return lambda x: x[start:]
    elif start is not None and end is not None and num is None:
        return lambda x: x[start:end]
    stride = (end - start) // num
    return lambda x: x[start:end:stride]

class LocDataset(data.Dataset):
    """
    mag dataset
    reading from a csv file
    """

    def __init__(self, dataroot, filename, stride=1, sampler=None):
        """
        init
        """
        print('constructing location dataset')
        self.dataroot = dataroot
        self.filename = filename
        self.stride = stride
        assert isinstance(stride, int) and stride > 0
        self.rawdata = np.loadtxt(
            os.path.join(self.dataroot, self.filename),
            dtype=np.str,
            delimiter=",")
        self.indexes = self.rawdata[:, 0]
        self.data = self.rawdata[:, 1:8].astype(np.float32)
        vfunc = np.vectorize(lambda x: x[:1] + '.' + x[1:])
        self.indexes = vfunc(self.indexes).astype(np.float64)
        self.data = self.data[np.argsort(self.indexes)]
        self.indexes.sort()
        self.indexes = self.indexes[::stride]
        self.data = torch.from_numpy(self.data[::stride]).float()

        # sample
        if sampler is not None:
            self.data = sampler(self.data)
            self.indexes = sampler(self.indexes)

        print("There are {} data".format(self.__len__()))
        assert self.indexes.shape[0] == self.data.size(0)

    def __len__(self):
        """
        len
        """
        return len(self.indexes)

    def __getitem__(self, idx):
        """
        get item
        return two numpy float array with shape (3) and (4)
        """
        # [location(3): x, y, z; orientation(4): Quaterion]
        # if batched, size:[B, 7]
        return self.data[idx]

class BootStrapLocDataset(LocDataset):
    """
    mag dataset
    reading from a csv file
    """
    def __init__(self, dataroot, filename, stride=1):
        """
        init
        """
        print('constructing bootstraping location dataset')
        super(BootStrapLocDataset, self).__init__(dataroot, filename, stride, None)
        self.bootstrap_idx = bootstrap(np.arange(self.indexes.shape[0]))
        self.data = self.data[self.bootstrap_idx]
        self.indexes = self.indexes[self.bootstrap_idx]

    def __len__(self):
        """
        len
        """
        return len(self.indexes)

    def __getitem__(self, idx):
        """
        get item
        return two numpy float array with shape (3) and (4)
        """
        return self.data[idx]

class RootBranchesDataset(data.Dataset):
    """
    root branches dataset
    """
    def __init__(self, root, branches, branches_len, branches_stride):
        """
        init
        """
        print('constructing root branches dataset')
        # Loc_dataset
        self.root = root
        # [image_dataset, mag_dataset]
        self.branches = branches
        self.branches_len = branches_len
        self.branches_stride = branches_stride
        print("There are {} data".format(self.__len__()))
        assert len(branches) == len(branches_len) == len(branches_stride)
        assert all(np.array(branches_len) > 0)

    @functools.lru_cache(maxsize=8192, typed=False)
    def __getitem__(self, idx):
        """
        get item
        """
        root_idx_val = self.root.indexes[idx]
        branches_indx_right = [bisect.bisect_left(x.indexes, root_idx_val) for x in self.branches]
        branches_indx_left = [x - 1 if x > 0 else 0 for x in branches_indx_right ]
        branches_time_left = [self.branches[i].indexes[branches_indx_left[i]] for i in range(len(self.branches))]
        branches_time_right = [self.branches[i].indexes[branches_indx_right[i]] for i in range(len(self.branches))]
        branches_time_err_left = np.abs(np.array(branches_time_left) - root_idx_val)
        branches_time_err_right = np.abs(np.array(branches_time_right) - root_idx_val)
        branches_indx = [branches_indx_left[i] if branches_time_err_left[i] < branches_time_err_right[i] else branches_indx_right[i] for i in range(len(self.branches))]
        branches_time = [self.branches[i].indexes[branches_indx[i]] for i in range(len(self.branches))]
        # calulate the error between obtained timestamp and original timestamp
        err = np.abs(np.array(branches_time) - root_idx_val)
        # if not all(err < 4e-10):
        #     print('root: {}, branches: {}, err: {}'.format(root_idx_val, branches_time, err))
        #     print('root idx: {}, branches idx: {}'.format(idx, branches_indx))
        #     print("left idx", branches_indx_left)
        #     print("right idx", branches_indx_right)
        #     print("left timestamp", branches_time_left)
        #     print("right, timestamp", branches_time_right)
        #     print("left error", branches_time_err_left)
        #     print("right error", branches_time_err_right)
        # assert all(err < 4e-10)
        def stack(arr, end, length, stride):
            # assert end - stride * length >= 0
            start = end - stride * (length - 1)
            if start < 0:
                start = 0
            return torch.stack([arr[i] for i in inclusive_range(start, end, stride)])
        # original stack
        # def stack(arr, end, length, stride):
        #     assert end - stride * length >= 0
        #     return torch.stack([arr[i] for i in range(end - stride * length, end, stride)])
        # return [Loc_data, stride * len before branches_indx image_data and mag_data] 3 tensors
        # return [7 datas, img_seq_len datas, meg_seq_len datas]
        return   [self.root[idx]] + [
            stack(
                x, branches_indx[i], self.branches_len[i], self.branches_stride[i])
            for i, x in enumerate(self.branches)]

    def __len__(self):
        """
        len
        """
        # upper_bound = [bisect.bisect_left(
        #     self.root.indexes,
        #     x.indexes[-self.branches_len[i]]) for i, x in enumerate(self.branches)]
        # upper_bound = [bisect.bisect_left(
        #     self.root.indexes,
        #     x.indexes[-1]) for i, x in enumerate(self.branches)]
        upper_bound = [find_closet_item(
            x.indexes[-1],
            self.root.indexes
            )[0] for i, x in enumerate(self.branches)]
        return min(upper_bound) + 1

class PartRootBranchesDataset(data.Dataset):
    """just randomly taking part of dataset"""
    def __init__(self, RootBranchesDataset, percentage=1):
        self.rootBranchesDataset = RootBranchesDataset
        self.percentage = percentage

        self.original_len = self.rootBranchesDataset.__len__()
        print("original length:", self.original_len)
        assert percentage <= 1
        self.new_len = int(self.original_len * percentage)
        print("{:.4f} percentage of original dataset".format(self.percentage))
        print("new length:", self.new_len)
        self.sample_idx = np.random.choice(list(range(self.original_len)), self.new_len, replace=False)

    def __getitem__(self, idx):
        """
        get item
        """
        return self.rootBranchesDataset.__getitem__(self.sample_idx[idx])

    def __len__(self):
        """
        len
        """
        return self.new_len

class TimestampRootBranchesDataset(data.Dataset):
    """docstring for ClassName"""
    def __init__(self, RootBranchesDataset):
        self.rootBranchesDataset = RootBranchesDataset

    def __getitem__(self, idx):
        """
        get item
        """
        return [self.rootBranchesDataset.root.indexes[idx]] + self.rootBranchesDataset.__getitem__(idx)

    def __len__(self):
        """
        len
        """
        return self.rootBranchesDataset.__len__()


class SeqRootBranchesDataset(data.Dataset):
    """
    root dataset using for out sequence
    """
    def __init__(self, loc_dataset, branches_dataset, branches_seq_len, branches_stride, config=None):
        """init root dataset
        :param loc_dataset:
        :param branches_dataset: two or one dataset(image or mag) eg. [imgDataset, magDataset]
        """
        print('constructing root branches dataset')
        assert len(branches_dataset) == len(branches_seq_len) == len(branches_stride)
        self.locdataset = loc_dataset
        self.branches_dataset = branches_dataset
        self.branches_seq_len = branches_seq_len
        self.branches_stride = branches_stride
        self.config = Config() if config is None else config
        print("There are {} data".format(self.__len__()))

    def __getitem__(self, item_idx):
        """
        :param item_idx: item_idx is the sequence's first(time-first) location's idx
        :return: [loc, img, mag] [tensor.size([LOC_LEN, 7]), tensor.size([IMG_LEN, C, H, W]), tensor.size([MAG_LEN, 3])]
        """

        # get location data
        start = item_idx
        stride = self.config.POINT_STRIDE
        end = start + self.config.POINT_STRIDE * (self.config.POINT_LEN - 1)
        assert end - stride <= len(self.locdataset)

        locs = torch.stack([self.locdataset.data[i] for i in inclusive_range(start, end, stride)])
        timestamps = [self.locdataset.indexes[i] for i in inclusive_range(start, end, stride)]  # use for search branch data

        # get branch data
        branch_data = []  # store all branch data
        for i, branch_dataset in enumerate(self.branches_dataset):
            # sample: keep end close enough, keep length [keep end and length strategy]
            # (consider the loc strategy : first start)
            end_idx, end_time, end_err = find_closet_item(timestamps[-1], branch_dataset.indexes)  # "last timing"
            start_idx = end_idx - (self.branches_seq_len[i] - 1) * self.branches_stride[i]
            stride = self.branches_stride[i]

            samples_idx = [i for i in inclusive_range(start_idx, end_idx, stride)]

            branch_data.append(torch.stack([branch_dataset[s] for s in samples_idx]))

        # [tensor.size([LOC_LEN, 7]), tensor.size([IMG_LEN, C, H, W]), tensor.size([MAG_LEN, 3])]
        # if batched, return sizes:
        # [tensor.size([B, LOC_LEN, 7]), tensor.size([B, IMG_LEN, C, H, W]), tensor.size([B, MAG_LEN, 3])]
        return [locs] + branch_data

    def __len__(self):
        # if keep end and length strategy
        # you may not worried about before 0 item no data in img and mag
        # first train loc timestamp in zhixian 1527408658841132544, val will be more after
        # mag
        # 15274086588——676 item
        # img
        # 15274086588——138 item
        # val last 1527409110321559296
        # mag total 45490
        # 152740911032——44999
        # img total 13824
        # 152740911032——13738
        # keep input data(img, mag) much more long enough than label (loc), you can not consider about the begin end
        # (^▽^ )
        return len(self.locdataset) - self.config.POINT_STRIDE * (self.config.POINT_LEN - 1)

class TimestampSeqRootBranchesDataset(SeqRootBranchesDataset):
    """
    root dataset using for out sequence, also output timestamp
    """
    def __init__(self, loc_dataset, branches_dataset, branches_seq_len, branches_stride, config=None):
        super(TimestampSeqRootBranchesDataset, self).__init__(loc_dataset,
                                                              branches_dataset,
                                                              branches_seq_len,
                                                              branches_stride,
                                                              config)

    def __getitem__(self, item_idx):
        """
        :param item_idx: item_idx is the sequence's first(time-first) location's idx
        :return: [loc, img, mag] [tensor.size([LOC_LEN, 7]), tensor.size([IMG_LEN, C, H, W]), tensor.size([MAG_LEN, 3])]
        """

        # get location data
        start = item_idx
        stride = self.config.POINT_STRIDE
        end = start + self.config.POINT_STRIDE * (self.config.POINT_LEN - 1)
        assert end - stride <= len(self.locdataset)

        timestamps = [self.locdataset.indexes[i] for i in inclusive_range(start, end, stride)]  # use for search branch data
        return [timestamps] + super(TimestampSeqRootBranchesDataset, self).__getitem__(item_idx)
