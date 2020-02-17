class EnumBranch:
    IMAGE_AND_MAG = 0
    IMAGE = 1
    MAG = 2

class Config(object):
    """Base configuration class. For custom configurations, create a
    sub-class that inherits from this one and override properties
    that need to be changed.
    """
    # Name the configurations. For example, 'COCO', 'Experiment 3', ...etc.
    # Useful if your code needs to do things differently depending on which
    # experiment is running.
    NAME = None  # Override in sub-classes

    # Dataset
    # input sequence length can not be the same as output length
    # we would sample them, just keep the begin point and the end point time close enough between input and output
    # inputs will time close enough
    # image sequence length  for each input
    IMG_LEN = 1
    # image sequence stride  for each input
    IMG_STRIDE = 1
    # magnetism sequence length  for each input
    MAG_LEN = 16
    # magnetism sequence stride  for each input
    MAG_STRIDE = 1

    # point sequence length for each output, that is output len
    POINT_LEN = 1
    # stride of location sequence
    # eg. if 1, return loc: 1,2,3;  if 2, return 1,3,5 ...
    POINT_STRIDE = 1

    BATCH_SIZE = 8
    EPOCH = 250
    LEARNING_RATE = 3e-4

    # model
    ENCODER_OUTPUT_SIZE = 256
    FUSION_OUTPUT_SIZE = 1024
    DECODER_OUTPUT_SIZE = 3 + 4 #location (xyz) and orientation(quaterion)

    IMG_RNN_NUM_LAYER = 1
    MAG_RNN_NUM_LAYER = 4

    BRANCH = EnumBranch.IMAGE_AND_MAG

    def __init__(self):
        """Set values of computed attributes."""
        pass

    def display(self):
        """Display Configuration values."""
        print("\nConfigurations:")
        for a in dir(self):
            # not built-in member variable and not function
            if not a.startswith("__") and not callable(getattr(self, a)): 
                print("{:30} {}".format(a, getattr(self, a)))
        print("\n")

    def get_dict(self):
        data_dict = dict()
        for a in dir(self):
            # not built-in member variable and not function
            if not a.startswith("__") and not callable(getattr(self, a)): 
                data_dict[a] = getattr(self, a)
        return data_dict