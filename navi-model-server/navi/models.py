"""
models
"""
import numpy as np
import torch
import torch.nn as nn
from .config import *

class ImageEncoder(nn.Module):
    """
    ImageEncoder
    """
    def __init__(self, pretrained_model):
        super(ImageEncoder, self).__init__()
        self.features = nn.Sequential(*list(pretrained_model.children())[:-1])
        self.lstm = nn.LSTM(256, 256, num_layers=1, batch_first=True)
        self.prep = nn.Sequential(
            nn.Linear(512, 256),
            nn.ReLU(inplace=True),
            nn.Linear(256, 256),
            nn.ReLU(inplace=True)
        )

    def forward(self, inpt):
        # [B, 1, 3, 224, 224]
        B, N, C, H, W = inpt.size()
        inpt = inpt.view(B * N, C, H, W)
        # [B*N, 3, 224, 224]
        f = self.features(inpt)
        # [B*N, 1, 1, 256]
        f = self.prep(f.view(B, N, -1))
        # [B, N, 256]
        # input shape should be (seq_len, batch, input_size), Oh, LSTM is set batch_first=True
        f, _ = self.lstm(f)
        # [B, N, 256]
        f = f[:, -1]
        # [B, 256]
        assert f.size(0) == B
        return f

class ToSeqImageEncoder(nn.Module):
    """
    ImageEncoder that output sequence: many to many and input length may not be same with output length
    """
    def __init__(self, pretrained_model, device=None, config=None):
        super(ToSeqImageEncoder, self).__init__()
        self.config = config if config is not None else Config()
        self.device = torch.device('cuda') if device is None else device
        self.feature_extractor = nn.Sequential(*list(pretrained_model.children())[:-1])
        self.lstm = nn.LSTM(input_size=256,
                            hidden_size=self.config.ENCODER_OUTPUT_SIZE,
                            num_layers=self.config.IMG_RNN_NUM_LAYER,
                            batch_first=True
                            )
        self.prep = nn.Sequential(
            nn.Linear(512, 256),
            nn.ReLU(inplace=True),
            nn.Linear(256, 256),
            nn.ReLU(inplace=True)
        )

    def forward(self, inpt):
        # [B, IMG_LEN, 3, 224, 224]
        B, N, C, H, W = inpt.size()
        inpt = inpt.view(B * N, C, H, W)
        # [B*IMG_LEN, 3, 224, 224] resnet input shape [B, C, H, W]
        f = self.feature_extractor(inpt)
        # [B*IMG_LEN, 1, 1, 512]
        f = f.view(B, N, -1)
        # [B, IMG_LEN, 512]
        f = self.prep(f)
        # [B, IMG_LEN, 256]
        # input shape should be (seq_len, batch, input_size), Oh, LSTM is set batch_first=True
        # so input shape (batch, seq_len, input_size)
        # get the max seq len as M
        M = self.config.IMG_LEN if self.config.IMG_LEN > self.config.POINT_LEN else self.config.POINT_LEN
        # pad with zeros
        pad = abs(N - M)
        f = torch.cat((f, torch.zeros(B, pad, 256).to(self.device)), 1)
        assert np.all(f[:, N:, :].cpu().detach().numpy() == 0)
        # [B, M, 256]
        f, _ = self.lstm(f)
        # [B, M, 256]
        f = f[:, M - self.config.POINT_LEN:, :]  # get last LOC_LEN item
        # [B, LOC_LEN, 256]
        return f

class MagEncoder(nn.Module):
    """
    MagEncoder
    """
    def __init__(self, input_size, hidden_size, num_layers):
        super(MagEncoder, self).__init__()
        self.gru = nn.GRU(input_size, hidden_size, num_layers, batch_first=True)
        self.bn = nn.BatchNorm1d(hidden_size)

    def forward(self, inpt):
        # [B, 1, 3 * mag_seq_len]
        output, _ = self.gru(inpt)
        # [B, 1, hidden_size] eg. [B, 1, 256]
        output = self.bn(output.view(output.size(0), -1))
        # [B, hidden_size] eg. [B, 256]
        return output

class WiFiEncoder(nn.Module):
    """
    WiFiEncoder
    """
    def __init__(self, input_size, hidden_size, num_layers):
        super(WiFiEncoder, self).__init__()
        self.gru = nn.GRU(input_size, hidden_size, num_layers, batch_first=True)
        self.bn = nn.BatchNorm1d(hidden_size)

    def forward(self, inpt):
        # [B, 1, 10]
        output, _ = self.gru(inpt)
        # [B, 1, hidden_size] eg. [B, 1, 256]
        output = self.bn(output.view(output.size(0), -1))
        # [B, hidden_size] eg. [B, 256]
        return output

class ToSeqMagEncoder(nn.Module):
    """
    MagEncoder that output sequence: many to many and input length may not be same with output length
    """
    def __init__(self, device=None, config=None):
        super(ToSeqMagEncoder, self).__init__()
        self.config = config if config is not None else Config()
        self.device = torch.device('cuda') if device is None else device
        self.gru = nn.GRU(3 * self.config.MAG_LEN,
                          self.config.ENCODER_OUTPUT_SIZE,
                          self.config.MAG_RNN_NUM_LAYER,
                          batch_first=True
                          )
        self.bn = nn.BatchNorm1d(self.config.POINT_LEN * 256)  # i don't know

    def forward(self, inpt):
        B = inpt.size(0)  # B not always equals to config.BATCH_SIZE
        # [B, mag_seq_len, 3]
        inpt = inpt.view(B, 1, -1)
        # [B, 1, 3 * mag_seq_len]
        inpt = inpt.expand([B, self.config.POINT_LEN, -1])
        assert inpt.size() == (B, self.config.POINT_LEN, 3 * self.config.MAG_LEN)
        # [B, LOC_LEN, 3 * mag_seq_len]
        output, _ = self.gru(inpt)
        # [B, LOC_LEN, hidden_size] eg. [B, 3, 256]
        output = output.contiguous().view(B, -1)
        output = self.bn(output)
        output = output.view(B, self.config.POINT_LEN, -1)
        # [B, LOC_LEN, hidden_size] eg. [B, 3, 256]
        return output

class Fusion(nn.Module):
    def __init__(self, input_size, output_size):
        super(Fusion, self).__init__()
        self.input_size = input_size
        self.output_size = output_size
        self.regressor = nn.Sequential(
            nn.Linear(sum(input_size), output_size),
            nn.ReLU(inplace=True),
        )

    def forward(self, inpt):
        assert len(inpt) == len(self.input_size)
        # ([B, 256],[B, 256])  or ([B, LOC_LEN, 256], [B, LOC_LEN, 256])
        return self.regressor(torch.cat(inpt, dim=-1))
        # [B, 1024]

    def fuse_weight(self):
        temp = self.regressor[0].weight.data.cpu().numpy().copy()
        mat_min, mat_max = np.min(temp), np.max(temp)
        output = 255 * (temp - mat_min) / (mat_max - mat_min + 1e-6)
        return output.astype(np.uint8)

class Decoder(nn.Module):
    def __init__(self, input_size, output_size):
        super(Decoder, self).__init__()
        self.input_size = input_size
        self.output_size = output_size
        self.regressor = nn.ModuleList(
            [nn.Sequential(nn.Linear(input_size, osize)) for osize in output_size])

    def forward(self, inpt):
        return [nn(inpt) for nn in self.regressor]

class MainModel(nn.Module):
    def __init__(self, encoders, fusion, decoder):
        super(MainModel, self).__init__()
        self.encoders = encoders if isinstance(encoders, list) else [encoders]
        print("encodes: ", [e.__doc__ for e in self.encoders])
        self.encoders = nn.ModuleList(self.encoders)
        self.fusion = fusion
        self.decoder = decoder

    def forward(self, inpt):
        inpt = inpt if isinstance(inpt, list) else [inpt]
        assert len(inpt) == len(self.encoders)
        code = [encode(x) for encode, x in zip(self.encoders, inpt)]
        fuse_result = self.fusion(code)
        output = self.decoder(fuse_result)
        return output
