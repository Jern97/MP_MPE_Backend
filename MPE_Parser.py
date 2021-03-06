from gmplot import GoogleMapPlotter
import webbrowser
import os
import sys
import json


# We subclass this just to change the map type
class CustomGoogleMapPlotter(GoogleMapPlotter):
    def __init__(self, center_lat, center_lng, zoom, apikey='',
                 map_type='satellite'):
        super().__init__(center_lat, center_lng, zoom, apikey)

        self.map_type = map_type
        assert (self.map_type in ['roadmap', 'satellite', 'hybrid', 'terrain'])

    def write_map(self, f):
        f.write('\t\tvar centerlatlng = new google.maps.LatLng(%f, %f);\n' %
                (self.center[0], self.center[1]))
        f.write('\t\tvar myOptions = {\n')
        f.write('\t\t\tzoom: %d,\n' % (self.zoom))
        f.write('\t\t\tcenter: centerlatlng,\n')

        # This is the only line we change
        f.write('\t\t\tmapTypeId: \'{}\'\n'.format(self.map_type))

        f.write('\t\t};\n')
        f.write(
            '\t\tvar map = new google.maps.Map(document.getElementById("map_canvas"), myOptions);\n')
        f.write('\n')


def rgb2hex(rgb):
    """ Convert RGBA or RGB to #RRGGBB """
    rgb = list(rgb[0:3])  # remove alpha if present
    #rgb = [int(c * 255) for c in rgb]
    hexcolor = '#%02x%02x%02x' % tuple(rgb)
    return hexcolor


def vibr2color(vibr):
    if vibr < 5:
        return rgb2hex([0,255,0])
    if vibr < 10:
        return rgb2hex([255,255,0])
    if vibr < 15:
        return rgb2hex([255,0,0])

    return rgb2hex([0,0,0])

def parse_file(file_name, show=False):
    lat = []
    lon = []
    vibr = []

    f = open("data/" + file_name, "r")
    data = json.load(f)
    for measurement in data['measurements']:
        lat.append(measurement['latitude'])
        lon.append(measurement['longitude'])
        vibr.append(measurement['measurement'])

    # Place map
    gmap = CustomGoogleMapPlotter(lat[0], lon[0], 15, map_type='satellite')
    # gmap.apikey = 'AIzaSyB81pS-FBw8d9QXcZ6iJk_WUb0khx_Ica0'

    # Polygon

    for i in range(1, len(lat)):
        gmap.plot(lat[i - 1:i + 1], lon[i - 1:i + 1], vibr2color(vibr[i]), edge_width=8)

    # Draw
    gmap.draw("html/" + file_name.split(".")[0] + ".html")

    if show:
        webbrowser.open('file://' + os.path.realpath('html/' + file_name.split(".")[0] + '.html'))


if __name__ == "__main__":
    parse_file(sys.argv[1], True)
