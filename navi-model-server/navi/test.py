import os
from .datasets import *
from .models import *
from .losses import *
import torchvision.transforms as transforms
from sklearn.preprocessing import StandardScaler

def train_deepnavi():
    # ### Congifuration
    pass

def test_deepnavi(model_path, 
                  device_index=0):
    ######################################
    ### Congifuration
    ######################################
    test_model_path = model_path
    tag = test_model_path.split("/")[-2]

    dataset_root = './dataset/office/'
    images_dir = './dataset/office/sensor_data/images/'
    mag_file = './dataset/office/sensor_data/geomagnetism/geomagnetism.csv'
    test_file = './dataset/office/validation/validation_data.csv'

    img_seq_len = 1
    img_seq_stride = 1
    mag_seq_len = 16
    mag_seq_stride = 1

    os.environ["CUDA_VISIBLE_DEVICES"] = str(device_index)
    device = torch.device('cuda')

    ######################################
    ### Data Preprocess
    ######################################
    normalize = transforms.Normalize(
        mean=[0.485, 0.456, 0.406],
        std=[0.229, 0.224, 0.225]
    )
    mag_scaler = StandardScaler()

    ######################################
    ### Dataset
    ######################################
    print("Dataset Constructing...")
    val_loc_dataset = LocDataset(dataroot='/home/huangjianjun/dataset/deepnavi/office/validation/', filename='validation_data.csv', stride=1)
    mag_dataset = MagDataset(file=mag_file, scaler=mag_scaler)
    img_dataset = ImageSeriesFolder(
        data_dir=images_dir,
        transform=transforms.Compose([
            transforms.Resize(256), # 将输入的PIL图片转换成给定的尺寸的大小
            transforms.CenterCrop(224),# 剪切并返回PIL图片上中心区域
            transforms.ToTensor(), # 将PIL图片或者numpy.ndarray转成Tensor类型的
            normalize # 用均值和标准差对张量图像进行标准化处理
        ]))
    val_dataset = RootBranchesDataset(
        root=val_loc_dataset,
        branches=[img_dataset, mag_dataset],
        branches_len=[img_seq_len, mag_seq_len],
        branches_stride=[img_seq_stride, mag_seq_stride])
    test_loader = torch.utils.data.DataLoader(
        val_dataset,
        batch_size=1, shuffle=False,
    )
    print("Dataset Constructed")

    ######################################
    ### Model
    ######################################
    image_encoder = ImageEncoder(models.resnet18(pretrained=True))
    mag_encoder = MagEncoder(3 * mag_seq_len, 256, 4)
    fusion = Fusion(input_size=[256, 256], output_size=1024)
    decoder = Decoder(input_size=1024, output_size=[3, 4])
    model = MainModel([image_encoder, mag_encoder], fusion, decoder)

    # load model weights
    model_weights = torch.load(model_path)
    model.load_state_dict(model_weights["state_dict"])
    model = model.to(device)

    ######################################
    ### Test 
    ######################################
    result_output_dir = os.path.join("./output/", tag)
    if not os.path.exists(result_output_dir):
        os.makedirs(result_output_dir)

    losses = AverageMeter()
    trans_losses = AverageMeterRecording()
    rotation_losses = AverageMeter()
    rotation_errors = AverageMeterRecording()
    trans_output_recoord = Recorder()
    rot_output_record = Recorder()
    model.eval()

    with torch.no_grad():
        for targets, imgs, mags in tqdm(test_loader):
            batch_size = mags.size(0)

            imgs, mags = imgs.to(device), mags.to(device)
            targets = targets.to(device)
            mags = mags.view(batch_size, 1, -1)

            trans_output, rotation_output = model([imgs, mags])

            trans_loss = pose_loss(trans_output, targets[:, 0:3])
            rotation_loss = pose_loss(rotation_output, targets[:, 3:])
            rot_err = rotation_error(rotation_output, targets[:, 3:])

            trans_losses.update(trans_loss.item(), batch_size)
            rotation_losses.update(rotation_loss.item(), batch_size)
            rotation_errors.update(rot_err.item(), batch_size)

            trans_output_recoord.add(trans_output.data, batched=True)
            rot_output_record.add(rotation_output.data, batched=True)

    trans_losses.to_file(os.path.join(result_output_dir, "trans_error.csv"))
    rotation_errors.to_file(os.path.join(result_output_dir, "rotation_error.csv"))
    trans_output_recoord.to_file(os.path.join(result_output_dir, "trans_estimation.csv"))
    rot_output_record.to_file(os.path.join(result_output_dir, "rot_estimation.csv"))

    test_trans_loss = trans_losses.avg
    test_rotation_loss = rotation_losses.avg
    test_rotation_err = rotation_errors.avg

    print(
        '[Test]  '
        'Trans Loss {:.3f}; '
        'Rotation Loss {:.3f}; '
        'Rotation Error {:.3f}; '
        ''.format(test_trans_loss, test_rotation_loss, test_rotation_err))


if __name__ == '__main__':
    test_deepnavi('./model_weights/sc/sc.pth.tar')