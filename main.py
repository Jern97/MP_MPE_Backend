from gmplot import GoogleMapPlotter
import webbrowser

# We subclass this just to change the map type
class CustomGoogleMapPlotter(GoogleMapPlotter):
    def __init__(self, center_lat, center_lng, zoom, apikey='',
                 map_type='satellite'):
        super().__init__(center_lat, center_lng, zoom, apikey)

        self.map_type = map_type
        assert(self.map_type in ['roadmap', 'satellite', 'hybrid', 'terrain'])

    def write_map(self,  f):
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
    value = rgb2hex([vibr/30, 0, 0])
    print(value)
    return value

lat = []
lon = []
vibr = []
speed = []

f = open("55515537.CSV", "r")
for line in f:
    variables = line.split(";")
    lat.append(float(variables[1]))
    lon.append(float(variables[2]))
    speed.append(float(variables[3]))
    vibr.append(float(variables[4]))



# Place map
gmap = CustomGoogleMapPlotter(51.155800, 3.196544, 15, map_type='satellite')
# Polygon


for i in range(1,len(lat)):
    print(lat[i-1:i+1])
    gmap.plot(lat[i-1:i+1], lon[i-1:i+1], vibr2color(vibr[i]), edge_width=8)

# Draw
gmap.draw("my_map.html")

webbrowser.open("my_map.html")