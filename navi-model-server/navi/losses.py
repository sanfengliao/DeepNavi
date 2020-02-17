"""
losses
"""
import math
import numpy as np
import torch
import torch.nn as nn

class GeometricLoss(nn.Module):
    def __init__(self, num_parameters=2, init=[0.0, -3.0]):
        self.num_parameters = num_parameters
        super(GeometricLoss, self).__init__()
        assert len(init) == num_parameters
        self.weight = nn.Parameter(torch.Tensor(np.array(init)))

    def forward(self, inpt):
        # inpt shape: [2, 1]
        # Lσ(I) = Lx(I) * exp(−sx)+sx+Lq(I) * exp(−sq)+sq
        return torch.sum(inpt * torch.exp(-self.weight)) + torch.sum(self.weight)

    def extra_repr(self):
        return 'num_parameters={}'.format(self.num_parameters)

def pose_loss(inpt, target):
    #  sqrt(sum(|inpt - target|^2)) / n
    # input, target shape: [batch_size, 4(quaternion) or 3(translation)]
    # pose_loss = 1/n * sum( L2_Norm )

    output = torch.norm(inpt - target, dim=1)
    output = torch.mean(output)
    return output

def pose_loss_seq(inpt, target):
    #  sqrt(sum(|inpt - target|^2)) / n
    # input, target shape: [batch_size, 4(quaternion) or 3(translation)]
    # pose_loss = 1/n * sum( L2_Norm )

    output = torch.norm(inpt - target, dim=2)
    output = torch.mean(output)
    return output

# def cosine_loss(inpt, target):
#     return torch.mean(1 - torch.sum(inpt * target, dim=-1))

def rotation_error(inpt, target):
    # θ=2 arccos(|⟨p,q⟩|)
    # with ⟨p, q⟩=p1q1+p2q2+p3q3+p4q4 and | ⋅ | the modulus function.
    # input, target shape: [batch_size, 4]

    # get mudule
    x1 = torch.norm(inpt, dim=1)
    x2 = torch.norm(target, dim=1)
    # normalize
    x1 = torch.div(inpt, torch.stack((x1, x1, x1, x1), dim=1))
    x2 = torch.div(target, torch.stack((x2, x2, x2, x2), dim=1))


    d = torch.abs(torch.sum(x1 * x2, dim=1))
    theta = 2 * torch.acos(d)
    # change to degree
    theta = theta * 180 / math.pi
    # mean of the batch
    theta = torch.mean(theta)
    return theta

def rotation_error_seq(inpt, target):
    # θ=2 arccos(|⟨p,q⟩|)
    # with ⟨p, q⟩=p1q1+p2q2+p3q3+p4q4 and | ⋅ | the modulus function.
    # input, target shape: [batch_size, 4]

    # get mudule
    x1 = torch.norm(inpt, dim=2)
    x2 = torch.norm(target, dim=2)
    # normalize
    x1 = torch.div(inpt, torch.stack((x1, x1, x1, x1), dim=2))
    x2 = torch.div(target, torch.stack((x2, x2, x2, x2), dim=2))


    d = torch.abs(torch.sum(x1 * x2, dim=2))
    theta = 2 * torch.acos(d)
    # change to degree
    theta = theta * 180 / math.pi
    # mean of the batch
    theta = torch.mean(theta)
    return theta