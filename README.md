После копирования на локальную машину для запуска сервера выполнить команды:
pip install Flask Flask-SQLAlchemy Flask-Migrate Flask-Cors opencv-python torch torchvision 
pip install -r requirements.txt
pip install pandas
pip install requests
python app.py
При последующих итерациях работы с серверной части достаточно выполнить python app.py

Для запуска клиентской части предварительно необходимо выбрать хост для подключения - в зависимости от того, будет ли отладка на эмуляторе или на реальном устройстве. ( См. RetrofitClient.kt). После чего достаточно нажать debug 'app' (Shift + F9) или run 'app' (Shift + F10).

Сейчас по нажатии кнопки "Сделать фотографию" переходим в режим камеры, после того, как снимок сделан, он сохраняется в bitmap внутри созданной viewmodel.  Происходит вызов функции upload - фото отправляется на сервер, где обрабатывается сетью yolo и результат распознавания пишется в базу бэка и функции fetchPhotosAndResults - которая получает результаты обработки и сохраняет их в базу мобилки. По окончании загрузки данных с сервера прелоадер скрывается и отображается картинка из viewModel и связанные с ней результаты в прокручиваемом списке.

Также доступна простая визуализация работы по сбору информации на фронтовой части по адресу, на котором развернут сервер.