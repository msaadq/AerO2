import googlemaps as gmaps
from googlemaps.exceptions import TransportError
from googleplaces import GooglePlaces, types, lang as gplaces
import math as m


class Maps:
    """
    This class handles all activity related to Google Maps / Google Places api and provides relevant data for
    the Properties Table
    """

    GOOGLE_API_KEY = "AIzaSyA5eHTwBbOEok1dyhQwum4Knd5Bir6nQPs"
    DEFAULT_ROAD_DISTANCE_THRESHOLD = 500
    DEFAULT_INDUSTRY_DISTANCE_THRESHOLD = 8000

    INDUSTRY_KEYWORDS = ['industry', 'factory', 'manufacturing', 'chemical', 'refinery', 'limited']

    _industries_coordinates = []
    _google_maps = None
    _google_places = None

    _lat_interval = 0.0
    _long_interval = 0.0

    _is_connected = False
    _industries_info = None

    def __init__(self):
        try:
            self._google_maps = gmaps.Client(key=self.GOOGLE_API_KEY)
            self._google_places = GooglePlaces(self.GOOGLE_API_KEY)
            self._is_connected = True
        except:
            self._is_connected = False

    def get_corner_coordinates(self, city_name):
        """
        These functions need to be defined here

        :param city_name: (String)

        :return corner_coordinates: (double[][])
        """

        try:
            geocode_result = self._google_maps.geocode(city_name)

        except Exception as e:
            return [[]]

        bounds = geocode_result[0]['geometry']['bounds']
        north_east = [bounds['northeast']['lat'], bounds['northeast']['lng']]
        south_west = [bounds['southwest']['lat'], bounds['southwest']['lng']]

        y1, y2, x1, x2 = north_east[0], south_west[0], south_west[1], north_east[1]

        corner_coordinates = [[y1, x1], [y1, x2], [y2, x2], [y2, x1]]

        return corner_coordinates

    def save_industries(self, city_name):
        for key in self.INDUSTRY_KEYWORDS:
            result = self._google_places.text_search(key + " in " + city_name)
            for place in result.places:
                self._industries_coordinates.append([float(place.geo_location['lat']), float(place.geo_location['lng'])])

        print "No. of industries in " + city_name + " : ", len(self._industries_coordinates)
        return self._industries_coordinates

    def get_altitude(self, node_coordinates):
        return self._google_maps.elevation((node_coordinates[0], node_coordinates[1]))[0]['elevation']

    def get_industry_index(self, coordinates):
        index = 0

        for ind_coordinates in self._industries_coordinates:
            dis = self.calc_distance_on_unit_sphere(ind_coordinates[0], ind_coordinates[1], coordinates[0], coordinates[1]) + 1

            if dis < self.DEFAULT_INDUSTRY_DISTANCE_THRESHOLD:
                index += 100.0 / float(dis)

        print "Industry Index: ", index

        return index

    def get_road_index(self, node_coordinates):
        y, x, lat, lon = node_coordinates[0], node_coordinates[1], self._lat_interval / 2, self._long_interval / 2
        road = []
        index = 0
        corners = [(y + lat, x - lon), (y + lat, x + lon), (y - lat, x + lon), (y - lat, x - lon)]
        sources, destinations = [], []

        try:
            query = self._google_maps.snap_to_roads(corners, interpolate=False)

            if len(query) < 2:
                raise ValueError
            for lists in query:
                road.append([lists['location']['latitude'], lists['location']['longitude']])

            if len(road) == 2:
                sources, destinations = [road[0]], [road[1]]
            elif len(road) == 3:
                sources, destinations = [road[0], road[1]], [road[1], road[2]]
            elif len(road) == 4:
                sources, destinations = [road[0], road[1], road[2]], [road[1], road[2], road[3]]

            rows = self._google_maps.distance_matrix(sources, destinations)['rows']

            for i in range(0, len(road) - 1):
                index += 100 * rows[i]['elements'][i]['duration']['value'] / ((float(
                        rows[i]['elements'][i]['distance']['value']) + 1) * float(
                        self.calc_distance_on_unit_sphere(y, x, road[i][0], road[i][1])))

            if len(road) > 2:
                index += 100 * rows[0]['elements'][len(road) - 2]['duration']['value'] / ((float(
                        rows[0]['elements'][len(road) - 2]['distance']['value']) + 1) * float(
                        self.calc_distance_on_unit_sphere(y, x, road[0][0], road[0][1])))
        except ValueError as e:
            index = 0
        except KeyError as e:
            index = 0
        except ZeroDivisionError as e:
            index = 25
        except TransportError as e:
            index = -1

        print "Road Index: ", index

        return index

    def set_intervals(self, lat_interval, long_interval):
        self._lat_interval = lat_interval
        self._long_interval = long_interval

    @staticmethod
    def calc_distance_on_unit_sphere(lat1, long1, lat2, long2):

        earth_radius = 6373000
        degrees_to_radians = m.pi / 180.0

        phi1 = (90.0 - lat1) * degrees_to_radians
        phi2 = (90.0 - lat2) * degrees_to_radians

        theta1 = long1 * degrees_to_radians
        theta2 = long2 * degrees_to_radians

        cos = (m.sin(phi1) * m.sin(phi2) * m.cos(theta1 - theta2) + m.cos(phi1) * m.cos(phi2))
        arc = m.acos(cos)

        return arc * earth_radius
