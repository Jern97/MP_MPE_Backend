from gmplot import GoogleMapPlotter
import webbrowser
import os

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
    rgb = [int(c * 255) for c in rgb]
    hexcolor = '#%02x%02x%02x' % tuple(rgb)
    return hexcolor


def vibr2color(vibr):
    value = rgb2hex([1 - vibr / 20, 0, 0])
    print(value)
    return value


def parse_file(file_name):
    lat = []
    lon = []
    vibr = []
    speed = []

    f = open("data/" + file_name, "r")
    for line in f:
        variables = line.split(";")
        lat.append(float(variables[1]))
        lon.append(float(variables[2]))
        speed.append(float(variables[3]))
        vibr.append(float(variables[4]))

    # Place map
    gmap = CustomGoogleMapPlotter(lat[0], lon[0], 15, map_type='satellite')
    # Polygon

    for i in range(1, len(lat)):
        print(lat[i - 1:i + 1])
        gmap.plot(lat[i - 1:i + 1], lon[i - 1:i + 1], vibr2color(vibr[i]), edge_width=8)

    # Draw
    html_file_name = file_name[:-4] + ".html"
    gmap.draw("html/" + html_file_name)

    webbrowser.open('file://' + os.path.realpath('html/' + html_file_name))


#parse_file(sys.argv[1])
