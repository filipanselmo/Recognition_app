import torch
from torch._C.cpp import nn
import tensorly as tl
from tensorly.decomposition import tucker
from typing import Tuple
from torchvision import transforms
from yolov5.utils.datasets import letterbox
from yolov5.utils.general import non_max_suppression


class OptimizedYOLO:
    def __init__(self):
        self.model = torch.hub.load('ultralytics/yolov5', 'yolov5s')
        # self.model.eval()

        # Квантование
        self.model = torch.quantization.quantize_dynamic(
            self.model, {torch.nn.Linear}, dtype=torch.qint8
        )

        # Сжатие через разложение Такера (Структурная аппроксимация)
        self.model = apply_tucker_decompression(self.model)

    def detect(self, image):
        img = self.preprocess(image)
        with torch.no_grad():
            pred = self.model(img)[0]
        pred = non_max_suppression(pred)
        return pred

    def preprocess(self, image):
        img = letterbox(image, new_shape=640)[0]
        img = transforms.ToTensor()(img)
        img = img.unsqueeze(0)
        return img


def apply_tucker_decompression(model: nn.Module, rank: float = 0.5) -> nn.Module:
    tl.set_backend('pytorch')

    for name, module in model.named_children():
        if isinstance(module, nn.Conv2d):
            # Применяем разложение Такера к сверточному слою
            compressed_module = tucker_decompose_conv(module, rank)
            setattr(model, name, compressed_module)
        else:
            # Рекурсивно обрабатываем вложенные модули
            apply_tucker_decompression(module, rank)
    return model


def tucker_decompose_conv(conv: nn.Conv2d, rank: float) -> nn.Module:
    """Заменяет сверточный слой на аппроксимированный с помощью разложения Такера"""
    weight = conv.weight.data

    # Преобразуем веса в тензор 4D (out_channels, in_channels, kH, kW)
    in_channels = weight.shape[1]
    out_channels = weight.shape[0]
    kernel_size = weight.shape[2:]

    # Разложение Такера
    core, factors = tucker(
        weight,
        rank=[int(rank * out_channels), int(rank * in_channels), *kernel_size],
        n_iter_max=20
    )

    # Создаем новые сверточные слои
    # Первый слой: (in_channels, new_in) с ядром 1x1
    first_conv = nn.Conv2d(
        in_channels=in_channels,
        out_channels=factors[1].shape[1],
        kernel_size=1,
        bias=False
    )
    first_conv.weight.data = factors[1].permute(1, 0, 2, 3)

    # Второй слой: (new_out, new_in) с исходным ядром
    second_conv = nn.Conv2d(
        in_channels=factors[1].shape[1],
        out_channels=factors[0].shape[1],
        kernel_size=kernel_size,
        padding=conv.padding,
        stride=conv.stride,
        bias=False
    )
    second_conv.weight.data = core.permute(0, 1, 2, 3)

    # Третий слой: (new_out, out_channels) с ядром 1x1
    third_conv = nn.Conv2d(
        in_channels=factors[0].shape[1],
        out_channels=out_channels,
        kernel_size=1,
        bias=False
    )
    third_conv.weight.data = factors[0].permute(1, 0, 2, 3)

    # Объединяем слои в последовательную сеть
    compressed_conv = nn.Sequential(
        first_conv,
        second_conv,
        third_conv
    )

    return compressed_conv