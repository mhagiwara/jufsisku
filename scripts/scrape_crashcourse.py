# coding=utf-8

import re
import sys
import urllib2
import codecs

sys.stdout = codecs.getwriter('utf_8')(sys.stdout)

response = urllib2.urlopen('https://mw.lojban.org/papri/The_Crash_Course_(a_draft)')
html = response.read()

for line in html.split('\n'):
    line = unicode(line, 'utf-8')
    m = re.search(ur'<dd><b>(.*?)</b> — <i>(.*?)</i></dd>', line)
    if m:
        print '%s\t%s' % (m.group(1), m.group(2))
