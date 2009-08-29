# Copyright (C) 2009 Benjamin C. Wiley Sittler (bsittler@gmail.com)
#
# This file is part of SarynPaint
#
# SarynPaint is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or (at
# your option) any later version.
#
# Saryn is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# for more details.
#
# You should have received a copy of the GNU General Public License
# along with SarynPaint.  If not, see <http://www.gnu.org/licenses/>.

import os
from sugar.activity import activity

class SarynPaintActivity(activity.Activity):
    def __init__(self, *args, **kw):
        activity.Activity.__init__(self, *args, **kw)
        self.connect('expose-event', self.__expose_cb)
        pass
    def __expose_cb(self, *args, **kw):
        os.system('java -jar sarynpaint.jar')
        self.close(True)
        pass
    pass
