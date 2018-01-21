
import os
from moviepy.editor import *
import time

def extract_frames(movie, imgdir):
    clip = VideoFileClip(movie)
    t = 0;
    while t<30:
         time.sleep(1)
         imgpath = os.path.join(imgdir, '{}.png'.format(t))
         clip.save_frame(imgpath, t)
         t += 1

movie = 'video.mp4'
imgdir = 'frames'

extract_frames(movie, imgdir)